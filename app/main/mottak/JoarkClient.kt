package mottak

interface JoarkClient {
    fun hentJournalpost(journalpostId: Long): Journalpost
}

class JoarkClientImpl(config: Config): JoarkClient {
    override fun hentJournalpost(journalpostId: Long): Journalpost {
        TODO("Not yet implemented")
    }
}