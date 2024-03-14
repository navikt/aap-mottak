package mottak.kafka

import libs.kafka.Topology
import libs.kafka.topology
import mottak.Config
import mottak.Journalpost
import mottak.arena.Arena
import mottak.arena.ArenaClient
import mottak.behandlingsflyt.Behandlingsflyt
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.enhet.EnhetService
import mottak.enhet.NavEnhet
import mottak.enhet.NorgClient
import mottak.enhet.SkjermingClient
import mottak.gosys.Gosys
import mottak.gosys.GosysClient
import mottak.joark.Joark
import mottak.joark.JoarkClient
import mottak.pdl.Pdl
import mottak.pdl.PdlClient
import mottak.saf.Saf
import mottak.saf.SafClient

private val IGNORED_MOTTAKSKANAL = listOf(
    "EESSI",
    "NAV_NO_CHAT",
    "EKST_OPPS"
)

class MottakTopology(
    config: Config,
    private val saf: Saf = SafClient(config),
    private val joark: Joark = JoarkClient(config),
    private val pdl: Pdl = PdlClient(config),
    private val kelvin: Behandlingsflyt = BehandlingsflytClient(config),
    private val arena: Arena = ArenaClient(config),
    private val gosys: Gosys = GosysClient(config),
    private val enhetService: EnhetService = EnhetService(NorgClient(config), SkjermingClient(config)),
) {
    private val topics = Topics(config.kafka)

    operator fun invoke(): Topology = topology {
        consume(topics.journalfoering)
            .secureLogWithKey { key, value -> warn("key: $key value: $value") }
            .filter { record -> record.mottaksKanal !in IGNORED_MOTTAKSKANAL }
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "M" }
            .map { record -> saf.hentJournalpost(record.journalpostId.toString()) }
            .filter { it.erSkjemaTilAAP() }
            .filter { !it.erJournalført() } // todo logg hvis dette skjer (topic skal være up-to-date med saf)
            .map { jp -> enrichWithNavEnhet(jp) }
            .forEach(::håndterJournalpost)
    }

    private fun enrichWithNavEnhet(journalpost: Journalpost): Pair<Journalpost, NavEnhet> {
        return when (journalpost) {
            is Journalpost.MedIdent -> {
                val personopplysninger = pdl.hentPersonopplysninger(journalpost.personident)
                @Suppress("NAME_SHADOWING")
                val journalpost = journalpost.copy(personident = personopplysninger.personident)
                journalpost to enhetService.getNavEnhet(personopplysninger)
            }
            is Journalpost.UtenIdent -> {
                journalpost to enhetService.getNavEnhetForFordelingsoppgave()
            }
        }
    }

    private fun håndterJournalpost(
        @Suppress("UNUSED_PARAMETER")
        key: String,
        journalpostMedEnhet: Pair<Journalpost, NavEnhet>,
    ) {
        val (journalpost, enhet) = journalpostMedEnhet

        when (journalpost) {
            is Journalpost.MedIdent -> håndterJournalpost(journalpost, enhet)
            is Journalpost.UtenIdent -> håndterJournalpost(journalpost)
        }

        joark.oppdaterJournalpost(journalpost, enhet)
    }

    private fun håndterJournalpost(journalpost: Journalpost.UtenIdent) {
        gosys.opprettOppgaveForManglendeIdent(journalpost)
    }

    private fun håndterJournalpost(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        if (journalpost.erPliktkort()) {
            error("not implemented")
        }

        if (arena.finnesSak(journalpost)) {
            gosys.opprettManuellJournalføringsoppgave(journalpost)
        } else if (skalTilKelvin(journalpost)) {
            kelvin.finnEllerOpprettSak(journalpost)
        } else {
            sakIkkeFunnet(journalpost, enhet)
        }

    }

    private fun skalTilKelvin(journalpost: Journalpost): Boolean {
        return false
    }

    private fun sakIkkeFunnet(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        when {
            journalpost.erEttersending() -> {
                gosys.opprettManuellJournalføringsoppgave(journalpost)
            }
            journalpost.erSøknad() -> arenaOppgave(journalpost, enhet)
            journalpost.erPliktkort() -> {}
            else -> error("Uhåndtert skjema for journalpostId ${journalpost.journalpostId}")
        }
    }

    private fun arenaOppgave(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        val saksnummer = arena.opprettOppgave(journalpost)
        gosys.opprettAutomatiskJournalføringsoppgave(journalpost, enhet)
    }
}
