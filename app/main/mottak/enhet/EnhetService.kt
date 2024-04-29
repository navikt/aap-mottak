package mottak.enhet

import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.pdl.Pdl
import mottak.pdl.Personopplysninger

class EnhetService(
    private val norg: Norg,
    private val skjerming: Skjerming,
    private val pdl: Pdl
) {
    fun enrichWithNavEnhet(journalpost: Journalpost): Pair<Journalpost, NavEnhet> {
        return when (journalpost) {
            is Journalpost.MedIdent -> {
                val personopplysninger = pdl.hentPersonopplysninger(journalpost.personident)
                val oppdatertJournalpost = journalpost.copy(personident = personopplysninger.personident)
                val enhet = oppdatertJournalpost.journalførendeEnhet ?: getNavEnhet(personopplysninger)
                oppdatertJournalpost to enhet
            }

            is Journalpost.UtenIdent -> {
                journalpost to getNavEnhetForFordelingsoppgave()
            }
        }
    }

    private fun getNavEnhet(personopplysninger: Personopplysninger): NavEnhet {
        val erSkjermet = skjerming.isSkjermet(personopplysninger.personident)

        val enhetsnrListe = norg.hentArbeidsfordeling(
            geografiskOmraade = personopplysninger.gt,
            skjermet = erSkjermet,
            gradering = personopplysninger.gradering,
        )

        if (enhetsnrListe.isEmpty()) {
            SECURE_LOG.warn("Fant ingen arbeidsfordeling for ${personopplysninger.personident.id}")
            return getNavEnhetForFordelingsoppgave()
        }

        return NavEnhet(enhetsnrListe.first().enhetNr)
    }

    private fun getNavEnhetForFordelingsoppgave(): NavEnhet {
        return NavEnhet("oslo") // TODO Sett rett her
    }
}

@JvmInline
value class NavEnhet(val nr: String)