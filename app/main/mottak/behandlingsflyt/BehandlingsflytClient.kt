package mottak.behandlingsflyt

import mottak.Config
import mottak.Journalpost

interface BehandlingsflytClient {
    fun finnes(journalpost: Journalpost): Boolean
    fun manuellJournaløring(journalpost: Journalpost)
}

class BehandlingsflytClientImpl(config: Config): BehandlingsflytClient {
    override fun finnes(journalpost: Journalpost): Boolean {
        TODO("Not yet implemented")
    }

    override fun manuellJournaløring(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

}
