package mottak.enhet

import mottak.pdl.Personopplysninger

class EnhetService(
    private val norg: Norg,
    private val skjerming: Skjerming,
) {
    fun getNavEnhet(personopplysninger: Personopplysninger): NavEnhet {
        val erSkjermet = skjerming.isSkjermet(personopplysninger.personident)

        val enhetsnrListe = norg.hentArbeidsfordeling(
            geografiskOmraade = personopplysninger.gt,
            skjermet = erSkjermet,
            gradering = personopplysninger.gradering,
        )

        if (enhetsnrListe.isEmpty()) error("Fant ingen arbeidsfordeling for ${personopplysninger.personident}")

        return NavEnhet(enhetsnrListe.first().enhetNr)
    }

    fun getNavEnhetForFordelingsoppgave(): NavEnhet {
        return NavEnhet("oslo")
    }
}

@JvmInline
value class NavEnhet(val nr: String)