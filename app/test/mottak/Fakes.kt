package mottak

import mottak.arena.Arena
import mottak.behandlingsflyt.Behandlingsflyt
import mottak.enhet.ArbeidsfordelingDtoResponse
import mottak.enhet.NavEnhet
import mottak.enhet.Norg
import mottak.enhet.Skjerming
import mottak.oppgave.Oppgave
import mottak.joark.Joark
import mottak.pdl.Pdl
import mottak.pdl.PdlGradering
import mottak.pdl.Personopplysninger
import mottak.saf.Saf

object JoarkFake : Joark {
    private val oppdaterteJournalposter = mutableListOf<Pair<Journalpost, NavEnhet>>()

    override fun oppdaterJournalpost(journalpost: Journalpost, enhet: NavEnhet) {
        oppdaterteJournalposter.add(journalpost to enhet)
    }

    fun harOppdatert(journalpostId: String, enhet: NavEnhet): Boolean {
        return oppdaterteJournalposter.any {
            it.first.journalpostId == journalpostId && it.second == enhet
        }
    }
}

object BehandlingsflytFake : Behandlingsflyt {
    override fun finnEllerOpprettSak(journalpost: Journalpost): Boolean {
        return false
    }

    override fun manuellJournaløring(journalpost: Journalpost) {
        TODO("Not yet implemented")
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
    private val saker = mutableListOf<String>()

    override fun finnesSak(journalpost: Journalpost.MedIdent): Boolean {
        return false
    }

    override fun opprettOppgave(journalpost: Journalpost.MedIdent) {
        if (saker.any { it == journalpost.journalpostId }) error("Oppgave finnes")
        saker.add(journalpost.journalpostId)
    }

    fun harOpprettetOppgaveMedId(id: String): Boolean {
        return saker.any { it == id }
    }

}

object OppgaveFake : Oppgave {
    private val automatiskeOppgaver = mutableListOf<Pair<String, NavEnhet>>()
    private val manuelleOppgaver = mutableListOf<String>()
    private val identitetsløseOppgaver = mutableListOf<String>()

    override fun opprettManuellJournalføringsoppgave(journalpost: Journalpost.MedIdent) {
        manuelleOppgaver.add(journalpost.journalpostId)
    }

    override fun opprettOppgaveForManglendeIdent(journalpost: Journalpost.UtenIdent) {
        identitetsløseOppgaver.add(journalpost.journalpostId)
    }

    override fun opprettAutomatiskJournalføringsoppgave(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        automatiskeOppgaver.add(journalpost.journalpostId to enhet)
    }

    fun harOpprettetAutomatiskOppgave(id: String, enhet: NavEnhet): Boolean {
        return automatiskeOppgaver.any { it.first == id && it.second == enhet }
    }
}

object SafFake : Saf {
    override fun hentJournalpost(journalpostId: String): Journalpost {
        return Journalpost.MedIdent(
            journalpostId = "123",
            erPliktkort = false,
            personident = Ident.Personident("1"),
            status = JournalpostStatus.MOTTATT,
            skjemanummer = SKJEMANUMMER_SØKNAD
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
