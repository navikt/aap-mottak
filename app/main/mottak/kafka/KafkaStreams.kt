package mottak.kafka

import mottak.Journalpost
import mottak.arena.ArenaClient
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.saf.SafClient
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.topology


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
            .filter { !it.erJournalført() }
            .filter { !it.erMeldekort() }
            .filter { it.erSøknadEllerEttersending() }
            .forEach { _, journalpost ->
                when (journalpost) {
                    is Journalpost.MedIdent -> håndter(journalpost, kelvin, arena, gosys)
                    is Journalpost.UtenIdent -> håndter(journalpost, gosys)
                }
            }
    }

fun håndter(
    journalpost: Journalpost.UtenIdent,
    gosys: GosysClient,
) {
    gosys.opprettOppgaveForManglendeIdent(journalpost)
}

fun håndter(
    journalpost: Journalpost.MedIdent,
    kelvin: BehandlingsflytClient,
    arena: ArenaClient,
    gosys: GosysClient,
) {
    when {
        journalpost.erPliktkort -> error("not implemented")
        arena.sakFinnes(journalpost) -> opprettOppgave(journalpost, gosys, arena)
        kelvin.finnes(journalpost) -> kelvin.manuellJournaløring(journalpost) // todo: oppgavestyring?
        else -> arena.opprettOppgave(journalpost) // todo: kelvin.journaløring(journalpost)
    }
}

private fun opprettOppgave(journalpost: Journalpost.MedIdent, gosys: GosysClient, arena: ArenaClient) {
    when (journalpost.erEttersending()) {
        true -> arena.opprettOppgave(journalpost)
        false -> gosys.opprettOppgave(journalpost)
    }
}

private val IGNORED_MOTTAKSKANAL = listOf(
    "EESSI",
    "NAV_NO_CHAT",
    "EKST_OPPS"
)
