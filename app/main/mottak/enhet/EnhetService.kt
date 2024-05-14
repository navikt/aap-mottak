package mottak.enhet

import mottak.Journalpost
import mottak.pdl.Pdl

class EnhetService(private val pdl: Pdl) {
    fun enrichWithNavEnhet(journalpost: Journalpost): Pair<Journalpost, NavEnhet> {
        return when (journalpost) {
            is Journalpost.MedIdent -> {
                journalpost to NavEnhet("9999")
            }

            is Journalpost.UtenIdent -> {
                journalpost to getNavEnhetForFordelingsoppgave()
            }
        }
    }

    private fun getNavEnhetForFordelingsoppgave(): NavEnhet {
        return NavEnhet("oslo") // TODO Sett rett her
    }
}

@JvmInline
value class NavEnhet(val nr: String)
