package mottak.kafka

import mottak.Journalpost
import mottak.arena.Arena
import mottak.behandlingsflyt.Behandlingsflyt
import mottak.enhet.EnhetService
import mottak.gosys.Gosys
import mottak.joark.Joark
import mottak.pdl.Pdl
import mottak.pdl.Personopplysninger
import mottak.saf.Saf
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.topology

private val IGNORED_MOTTAKSKANAL = listOf(
    "EESSI",
    "NAV_NO_CHAT",
    "EKST_OPPS"
)

class MottakTopology(
    private val saf: Saf,
    private val joark: Joark,
    private val pdl: Pdl,
    private val kelvin: Behandlingsflyt,
    private val arena: Arena,
    private val gosys: Gosys,
    private val enhetService: EnhetService,
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
            arena.finnesSak(journalpost) -> gosys.opprettManuellJournalføringsoppgave(journalpost)
            kelvin.finnesSak(journalpost) -> kelvin.manuellJournaløring(journalpost)
            else -> sakIkkeFunnet(journalpost, personopplysninger)
        }
    }

    private fun sakIkkeFunnet(journalpost: Journalpost.MedIdent, opplysninger: Personopplysninger) {
        when {
            journalpost.erEttersending() -> gosys.opprettManuellJournalføringsoppgave(journalpost)
            journalpost.erSøknad() -> arenaOppgave(journalpost, opplysninger)
            else -> error("Ukjent hva man skal gjøre")
        }
    }

    private fun arenaOppgave(journalpost: Journalpost.MedIdent, opplysninger: Personopplysninger) {
        val saksnummer = arena.opprettOppgave(journalpost)
        val enhet = enhetService.getNavEnhet(opplysninger)
        gosys.opprettAutomatiskJournalføringsoppgave(journalpost, enhet)
    }
}
