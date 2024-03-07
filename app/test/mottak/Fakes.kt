package mottak

import mottak.arena.ArenaClient
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.kafka.SKJEMANUMMER_SØKNAD
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

    override fun sakFinnes(journalpost: Journalpost): Boolean {
        return false
    }

    override fun opprettOppgave(journalpost: Journalpost) {
        if (sakliste.any { it == journalpost.journalpostId }) error("Oppgave finnes")
        sakliste.add(journalpost.journalpostId)
    }

    fun harOpprettetOppgaveMedId(id: String): Boolean {
        return sakliste.any { it == id}
    }

}

class GosysClientFake : GosysClient {
    override fun opprettOppgave(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

    override fun opprettOppgaveForManglendeIdent(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

}

class SafClientFake : SafClient {
    override fun hentJournalpost(journalpostId: String): Journalpost {
        return Journalpost(
            journalpostId = "123",
            erMeldekort = false,
            erPliktkort = false,
            personident = "1",
            status = JournalpostStatus.MOTTATT,
            bruker = "",
            skjemanummer = SKJEMANUMMER_SØKNAD
        )
    }
}

