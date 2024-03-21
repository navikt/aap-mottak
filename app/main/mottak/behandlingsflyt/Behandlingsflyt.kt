package mottak.behandlingsflyt

import mottak.Config
import mottak.Journalpost
import java.util.*

interface Behandlingsflyt {
    fun finnEllerOpprettSak(journalpost: Journalpost): String
    fun manuellJournaløring(journalpost: Journalpost)
}

class BehandlingsflytClient(config: Config) : Behandlingsflyt {
    override fun finnEllerOpprettSak(journalpost: Journalpost): String {
        return UUID.randomUUID().toString()
    }

    override fun manuellJournaløring(journalpost: Journalpost) {
        TODO("Not yet implemented")
    }

}
