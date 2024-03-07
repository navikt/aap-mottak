package mottak

import mottak.arena.ArenaClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.saf.SafClient
import no.nav.aap.kafka.serde.avro.AvroSerde
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.serde.StreamSerde
import no.nav.aap.kafka.streams.v2.topology
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

fun createTopology(
    saf: SafClient,
    joark: JoarkClient,
    kelvin: BehandlingsflytClient,
    arena: ArenaClient,
    gosys: GosysClient,
): Topology =
    topology {
        consume(journalfoering)
            .filter { record -> record.mottaksKanal !in IGNORED_MOTTAKSKANAL }
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "MOTTATT" }
            .map { _, record -> saf.hentJournalpost(record.journalpostId.toString()) }
            .filter { journalpost -> journalpost.status != JournalpostStatus.JOURNALFØRT }
            .forEach { _, journalpost ->
                when {
                    journalpost.erMeldekort -> error("not implemented")
                    journalpost.bruker == null -> gosys.opprettOppgave(journalpost)
                    arena.sakFinnes(journalpost) -> arena.opprettOppgave(journalpost)
                    kelvin.finnes(journalpost) -> kelvin.manuellJournaløring(journalpost) // todo: oppgavestyring?
                    else -> arena.opprettOppgave(journalpost) // todo: kelvin.journaløring(journalpost)
                }
            }
    }

private val journalfoering = Topic(
    name = "teamdokumenthandtering.aapen-dok-journalfoering",
    valueSerde = joarkAvroSerde(),
)

private val IGNORED_MOTTAKSKANAL = listOf(
    "EESSI",
    "NAV_NO_CHAT",
    "EKST_OPPS"
)

private fun joarkAvroSerde(): StreamSerde<JournalfoeringHendelseRecord> =
    object : StreamSerde<JournalfoeringHendelseRecord> {
        private val internal = AvroSerde.specific<JournalfoeringHendelseRecord>()
        override fun serializer(): Serializer<JournalfoeringHendelseRecord> = internal.serializer()
        override fun deserializer(): Deserializer<JournalfoeringHendelseRecord> = internal.deserializer()
    }
