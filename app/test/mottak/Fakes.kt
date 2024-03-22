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
import java.util.*

object JoarkFake : Joark {
    private val oppdaterteJournalposter = mutableListOf<Pair<Journalpost, NavEnhet>>()

    override fun oppdaterJournalpost(journalpost: Journalpost, enhet: NavEnhet, fagsakId: String) {
        oppdaterteJournalposter.add(journalpost to enhet)
    }

    fun harOppdatert(journalpostId: Long, enhet: NavEnhet): Boolean {
        return oppdaterteJournalposter.any {
            it.first.journalpostId == journalpostId && it.second == enhet
        }
    }
}

object BehandlingsflytFake : Behandlingsflyt {
    private val saker = mutableListOf<Journalpost>()

    override fun finnEllerOpprettSak(journalpost: Journalpost.MedIdent): Saksinfo {
        saker.add(journalpost)
        return Saksinfo(UUID.randomUUID().toString(), Periode(LocalDate.now(), LocalDate.now()))
    }

    override fun sendSøknad(sakId: String, journalpostId: Long, søknad: ByteArray) {
        TODO("Not yet implemented")
    }

    fun harOpprettetSak(journalpostId: Long): Boolean {
        return saker.any { it.journalpostId == journalpostId }
    }
}

object SkjermingFake : Skjerming {
    override fun isSkjermet(personident: Ident.Personident): Boolean {
        return false
    }
}

object NorgFake : Norg {
    override fun hentArbeidsfordeling(
        geografiskOmraade: String,
        skjermet: Boolean,
        gradering: PdlGradering
    ): List<ArbeidsfordelingDtoResponse> {
        return listOf(
            ArbeidsfordelingDtoResponse(enhetNr = "oslo")
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
            erPliktkort = false,
            personident = Ident.Personident("1"),
            status = JournalpostStatus.MOTTATT,
            journalførendeEnhet = NavEnhet("oslo"),
            skjemanummer = SKJEMANUMMER_SØKNAD,
            mottattDato = LocalDate.now()
        )
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
