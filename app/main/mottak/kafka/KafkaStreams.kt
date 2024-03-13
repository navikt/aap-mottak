package mottak.kafka

import mottak.Journalpost
import mottak.arena.ArenaClient
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.pdl.PdlClient
import mottak.saf.SafClient
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.topology

private val IGNORED_MOTTAKSKANAL = listOf(
    "EESSI",
    "NAV_NO_CHAT",
    "EKST_OPPS"
)

class MottakTopology(
    private val saf: SafClient,
    private val joark: JoarkClient,
    private val pdl: PdlClient,
    private val kelvin: BehandlingsflytClient,
    private val arena: ArenaClient,
    private val gosys: GosysClient,
) {
    operator fun invoke(): Topology = topology {
        consume(Topics.journalfoering)
            .filter { record -> record.mottaksKanal !in IGNORED_MOTTAKSKANAL }
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "MOTTATT" }
            .map { _, record -> saf.hentJournalpost(record.journalpostId.toString()) }
            .filter { !it.erJournalført() }
            .filter { it.erSøknadEllerEttersending() }
            .forEach(::håndterJournalpost)
    }

    private fun håndterJournalpost(
        @Suppress("UNUSED_PARAMETER")
        key: String,
        journalpost: Journalpost,
    ) {
        when (journalpost) {
            is Journalpost.MedIdent -> håndterJournalpost(journalpost)
            is Journalpost.UtenIdent -> håndterJournalpost(journalpost)
        }
    }

    private fun håndterJournalpost(journalpost: Journalpost.UtenIdent) {
        gosys.opprettOppgaveForManglendeIdent(journalpost)
    }

    private fun håndterJournalpost(journalpost: Journalpost.MedIdent) {
        val personopplysninger = pdl.hentPersonopplysninger(journalpost.personident)

        @Suppress("NAME_SHADOWING")
        val journalpost = journalpost.copy(personident = personopplysninger.personident)

        when {
            journalpost.erPliktkort() -> error("not implemented")
            arena.sakFinnes(journalpost) -> opprettOppgave(journalpost, gosys, arena)
            kelvin.finnes(journalpost) -> kelvin.manuellJournaløring(journalpost)
            else -> arena.opprettOppgave(journalpost)
        }
    }

    private fun opprettOppgave(journalpost: Journalpost.MedIdent, gosys: GosysClient, arena: ArenaClient) {
        when (journalpost.erEttersending()) {
            true -> arena.opprettOppgave(journalpost)
            false -> gosys.opprettOppgave(journalpost)
        }
    }
}
