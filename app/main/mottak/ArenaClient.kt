package mottak

interface ArenaClient {
    fun finnes(journalpost: Journalpost): Boolean
    fun journalføring(journalpost: Journalpost)
    fun manuellJournaløring(journalpost: Journalpost)
}

class ArenaClientImpl(config: Config): ArenaClient {
    override fun finnes(journalpost: Journalpost): Boolean {
        TODO("Not yet implemented")
    }

    override fun journalføring(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

    override fun manuellJournaløring(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

}