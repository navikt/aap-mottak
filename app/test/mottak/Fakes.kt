package mottak

import mottak.arena.Arena
import mottak.behandlingsflyt.Behandlingsflyt
import mottak.behandlingsflyt.Periode
import mottak.behandlingsflyt.Saksinfo
import mottak.enhet.ArbeidsfordelingDtoResponse
import mottak.enhet.NavEnhet
import mottak.enhet.Norg
import mottak.enhet.Skjerming
import mottak.joark.Joark
import mottak.oppgave.Oppgave
import mottak.pdl.Pdl
import mottak.pdl.PdlGradering
import mottak.pdl.Personopplysninger
import mottak.saf.Saf
import java.time.LocalDate
import java.util.UUID

object JoarkFake : Joark {
    private val oppdaterteJournalposter = mutableListOf<Pair<Journalpost, NavEnhet>>()
    private val ferdigstilteJournalposter = mutableListOf<Pair<Journalpost, NavEnhet>>()

    override fun oppdaterJournalpost(journalpost: Journalpost.MedIdent, fagsakId: String) {
        oppdaterteJournalposter.add(journalpost to NavEnhet("9999"))
    }

    override fun ferdigstillJournalpost(journalpost: Journalpost) {
        ferdigstilteJournalposter.add(journalpost to NavEnhet("9999"))
    }

    fun harOppdatert(journalpostId: Long, enhet: NavEnhet): Boolean {
        return oppdaterteJournalposter.any {
            it.first.journalpostId == journalpostId && it.second == enhet
        }
    }

    fun harFerdigstilt(journalpostId: Long): Boolean {
        return ferdigstilteJournalposter.any {
            it.first.journalpostId == journalpostId
        }
    }
}

object BehandlingsflytFake : Behandlingsflyt {
    private val saker = mutableListOf<Journalpost>()
    private val søknader = mutableListOf<Pair<String, Long>>()

    override fun finnEllerOpprettSak(journalpost: Journalpost.MedIdent): Saksinfo {
        saker.add(journalpost)
        return Saksinfo(UUID.randomUUID().toString(), Periode(LocalDate.now(), LocalDate.now()))
    }

    override fun sendSøknad(sakId: String, journalpostId: Long, søknad: ByteArray) {
        søknader.add(sakId to journalpostId)
    }

    fun harOpprettetSak(journalpostId: Long): Boolean {
        return saker.any { it.journalpostId == journalpostId }
    }

    fun harSendtSøknad(sakId: String, journalpostId: Long): Boolean {
        return søknader.any {
            it.first == sakId && it.second == journalpostId
        }
    }
}

object SkjermingFake : Skjerming {
    override fun isSkjermet(personident: Ident.Personident): Boolean {
        return false
    }
}

object NorgFake : Norg {
    const val ENHET_NR = "3001"
    override fun hentArbeidsfordeling(
        geografiskOmraade: String,
        skjermet: Boolean,
        gradering: PdlGradering
    ): List<ArbeidsfordelingDtoResponse> {
        return listOf(
            ArbeidsfordelingDtoResponse(enhetNr = ENHET_NR)
        )
    }
}

object ArenaFake : Arena {
    private val saker = mutableListOf<Long>()

    override fun finnesSak(journalpost: Journalpost.MedIdent): Boolean {
        return false
    }

    override fun opprettOppgave(journalpost: Journalpost.MedIdent) {
        if (saker.any { it == journalpost.journalpostId }) error("Oppgave finnes")
        saker.add(journalpost.journalpostId)
    }

    fun harOpprettetOppgaveMedId(journalpostId: Long): Boolean {
        return saker.any { it == journalpostId }
    }

}

object OppgaveFake : Oppgave {
    private val automatiskeOppgaver = mutableListOf<Pair<Long, NavEnhet>>()
    private val manuelleOppgaver = mutableListOf<Long>()
    private val identitetsløseOppgaver = mutableListOf<Long>()

    override fun opprettManuellJournalføringsoppgave(journalpost: Journalpost.MedIdent) {
        manuelleOppgaver.add(journalpost.journalpostId)
    }

    override fun opprettOppgaveForManglendeIdent(journalpost: Journalpost.UtenIdent) {
        identitetsløseOppgaver.add(journalpost.journalpostId)
    }

    override fun opprettAutomatiskJournalføringsoppgave(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        automatiskeOppgaver.add(journalpost.journalpostId to enhet)
    }

    fun harOpprettetAutomatiskOppgave(journalpostId: Long, enhet: NavEnhet): Boolean {
        return automatiskeOppgaver.any { it.first == journalpostId && it.second == enhet }
    }
}

object SafFake : Saf {
    override fun hentJournalpost(journalpostId: Long): Journalpost {
        return Journalpost.MedIdent(
            journalpostId = 123,
            personident = Ident.Personident("1"),
            status = JournalpostStatus.MOTTATT,
            journalførendeEnhet = NavEnhet(NorgFake.ENHET_NR),
            mottattDato = LocalDate.now(),
            dokumenter = listOf(
                Dokument(
                    dokumentInfoId = "1234",
                    brevkode = SKJEMANUMMER_SØKNAD,
                    variantFormat = "ORIGINAL"
                )
            )
        )
    }

    override fun hentJson(journalpostId: Long): ByteArray? {
        return null
    }
}

object PdlFake : Pdl {
    override fun hentPersonopplysninger(ident: Ident): Personopplysninger {
        return Personopplysninger(
            personident = Ident.Personident("1"),
            gradering = PdlGradering.UGRADERT,
            gt = "1234"
        )
    }
}
