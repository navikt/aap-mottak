package mottak.enhet

import mottak.Config
import mottak.Ident

class EnhetService(config: Config) {
    private val norg = NorgClient(config)
    private val skjerming = SkjermingClient(config)
    private val pdl = PdlClient(config)

    fun getNavenhetForBruker(ident: Ident): String {
        val personopplysninger = pdl.hentPersonopplysninger(ident)
        val personident = personopplysninger.personident
        val erSkjermet = skjerming.isSkjermet(personident)
        val enhetsnrListe = norg.hentArbeidsfordeling(personopplysninger.gt, erSkjermet, personopplysninger.gradering)

        if (enhetsnrListe.isEmpty()) error("Fant ingen arbeidsfordeling for $personident")

        return enhetsnrListe.first().enhetNr
    }
}
