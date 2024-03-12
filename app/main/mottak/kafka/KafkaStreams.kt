package mottak.kafka

import mottak.Ident
import mottak.Journalpost
import mottak.arena.ArenaClient
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.pdl.PdlClient
import mottak.saf.SafClient
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.stream.MappedStream
import no.nav.aap.kafka.streams.v2.topology

fun createTopology(
    saf: SafClient,
    joark: JoarkClient,
    pdl: PdlClient,
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
            .filter { it.erSøknadEllerEttersending() }
            .branch({ it is Journalpost.UtenIdent }, håndterUtenIdent(gosys))
            .branch({ it is Journalpost.MedIdent }, håndterMedIdent(kelvin, pdl, arena, gosys))
            .default {
                forEach { key, value ->
                    error("Uhåndter type journalpost for kafka key $key type: ${value::class.simpleName}")
                }
            }
    }

private fun håndterUtenIdent(
    gosys: GosysClient,
): (MappedStream<Journalpost>) -> Unit = { journalposter ->
    journalposter.forEach { _, journalpost ->
        gosys.opprettOppgaveForManglendeIdent(journalpost as Journalpost.UtenIdent)
    }
}

private fun håndterMedIdent(
    kelvin: BehandlingsflytClient,
    pdl: PdlClient,
    arena: ArenaClient,
    gosys: GosysClient
): (MappedStream<Journalpost>) -> Unit = { journalposter ->
    journalposter.forEach { _, journalpost ->

        val personopplysninger = pdl.hentPersonopplysninger(
            ident = (journalpost as Journalpost.MedIdent).personident
        )

        val journalpostMedAktivIdent = journalpost.copy(
            personident = Ident.Personident(personopplysninger.personident)
        )

        when {
            journalpostMedAktivIdent.erPliktkort -> error("not implemented")
            arena.sakFinnes(journalpostMedAktivIdent) -> opprettOppgave(journalpostMedAktivIdent, gosys, arena)
            kelvin.finnes(journalpostMedAktivIdent) -> kelvin.manuellJournaløring(journalpostMedAktivIdent)
            else -> arena.opprettOppgave(journalpostMedAktivIdent)
        }
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
