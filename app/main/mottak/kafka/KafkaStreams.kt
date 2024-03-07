package mottak.kafka

import mottak.Journalpost
import mottak.JournalpostStatus
import mottak.arena.ArenaClient
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.saf.SafClient
import mottak.saf.SafClientImpl
import no.nav.aap.kafka.serde.avro.AvroSerde
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.serde.StreamSerde
import no.nav.aap.kafka.streams.v2.topology
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

const val SKJEMANUMMER_SØKNAD = "NAV 11-13.05"
const val SKJEMANUMMER_SØKNAD_ETTERSENDING = "NAVe 11-13.05"

fun createTopology(
    saf: SafClient,
    joark: JoarkClient,
    kelvin: BehandlingsflytClient,
    arena: ArenaClient,
    gosys: GosysClient,
): Topology =
    topology {
        consume(Topics.journalfoering)
            .filter { record -> record.mottaksKanal !in IGNORED_MOTTAKSKANAL }
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "MOTTATT" }
            .map { _, record -> saf.hentJournalpost(record.journalpostId.toString()) }
            .filter { journalpost -> journalpost.status != JournalpostStatus.JOURNALFØRT } // TODO Error hvis dette slår til?
            .filter { journalpost -> !journalpost.erMeldekort }
            .filter { journalpost -> journalpost.skjemanummer in listOf(SKJEMANUMMER_SØKNAD, SKJEMANUMMER_SØKNAD_ETTERSENDING) }
            .forEach { _, journalpost ->
                when {
                    journalpost.erPliktkort -> error("not implemented")
                    journalpost.bruker == null -> gosys.opprettOppgaveForManglendeIdent(journalpost)
                    arena.sakFinnes(journalpost) -> opprettOppgave(journalpost, gosys, arena)
                    kelvin.finnes(journalpost) -> kelvin.manuellJournaløring(journalpost) // todo: oppgavestyring?
                    else -> arena.opprettOppgave(journalpost) // todo: kelvin.journaløring(journalpost)
                }
            }
    }

private fun opprettOppgave(journalpost: Journalpost, gosys: GosysClient, arena: ArenaClient) {
    if (journalpost.skjemanummer == SKJEMANUMMER_SØKNAD_ETTERSENDING) {
        arena.opprettOppgave(journalpost)
    } else {
        gosys.opprettOppgave(journalpost)
    }
}

private val IGNORED_MOTTAKSKANAL = listOf(
    "EESSI",
    "NAV_NO_CHAT",
    "EKST_OPPS"
)
