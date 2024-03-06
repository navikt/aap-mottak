package mottak

interface GosysClient {
    fun manuellJournaløring(journalpost: Journalpost)
}

class GosysClientImpl(config: Config): GosysClient {
    override fun manuellJournaløring(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }
}
