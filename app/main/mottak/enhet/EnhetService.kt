package mottak.enhet

import mottak.pdl.Personopplysninger

class EnhetService(
    private val norg: Norg,
    private val skjerming: Skjerming,
) {
    fun getNavEnhet(personopplysninger: Personopplysninger): String {
        val erSkjermet = skjerming.isSkjermet(personopplysninger.personident)

        val enhetsnrListe = norg.hentArbeidsfordeling(
            geografiskOmraade = personopplysninger.gt,
            skjermet = erSkjermet,
            gradering = personopplysninger.gradering,
        )

        if (enhetsnrListe.isEmpty()) error("Fant ingen arbeidsfordeling for ${personopplysninger.personident}")

        return enhetsnrListe.first().enhetNr // TODO: er dette riktig api-endepunkt?
    }
}
