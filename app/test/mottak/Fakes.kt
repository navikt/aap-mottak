package mottak

import mottak.arena.ArenaClient
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.pdl.PdlClient
import mottak.pdl.PdlGradering
import mottak.pdl.Personopplysninger
import mottak.saf.SafClient

class JoarkClientFake : JoarkClient {
    override fun ferdigstill(journalpostId: Long) {
        TODO("Not yet implemented")
    }
}

class BehandlingsflytClientFake : BehandlingsflytClient {
    override fun finnes(journalpost: Journalpost): Boolean {
        return false
    }

    override fun manuellJournaløring(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

}

class ArenaClientFake : ArenaClient {
    private val sakliste = mutableListOf<String>()

    override fun sakFinnes(journalpost: Journalpost.MedIdent): Boolean {
        return false
    }

    override fun opprettOppgave(journalpost: Journalpost.MedIdent) {
        if (sakliste.any { it == journalpost.journalpostId }) error("Oppgave finnes")
        sakliste.add(journalpost.journalpostId)
    }

    fun harOpprettetOppgaveMedId(id: String): Boolean {
        return sakliste.any { it == id}
    }

}

class GosysClientFake : GosysClient {
    override fun opprettOppgave(journalpost: Journalpost.MedIdent) {
        TODO("Not yet implemented")
    }

    override fun opprettOppgaveForManglendeIdent(journalpost: Journalpost.UtenIdent) {
        TODO("Not yet implemented")
    }

}

class SafClientFake : SafClient {
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

class PdlClientFake: PdlClient {
    override fun hentPersonopplysninger(ident: Ident): Personopplysninger {
        return Personopplysninger(
            personident = Ident.Personident("1"),
            gradering = PdlGradering.UGRADERT,
            gt = "1234"
        )
    }

}
