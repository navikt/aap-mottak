package mottak.behandlingsflyt

import mottak.Config
import mottak.Journalpost

interface Behandlingsflyt {
    fun finnesSak(journalpost: Journalpost): Boolean
    fun manuellJournaløring(journalpost: Journalpost)
}

class BehandlingsflytClient(config: Config): Behandlingsflyt {
    override fun finnesSak(journalpost: Journalpost): Boolean {
        TODO("Not yet implemented")
    }

    override fun manuellJournaløring(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

}
