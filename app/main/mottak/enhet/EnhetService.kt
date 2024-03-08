package mottak.enhet

import mottak.Config
import mottak.Ident

class EnhetService(config: Config) {
    private val norg = NorgClient(config)
    private val skjerming = SkjermingClient(config)
    private val pdl = PdlClient(config)

    fun getNavenhetForBruker(personident: Ident): String {
        val gtOgGradering = pdl.hentGTogGradering(personident)
        val erSkjermet = skjerming.isSkjermet(personident)
        val enhetsnrListe = norg.hentArbeidsfordeling(gtOgGradering.gt, erSkjermet, gtOgGradering.gradering)

        if (enhetsnrListe.isEmpty()) error("Fant ingen arbeidsfordeling for $personident")

        return enhetsnrListe.first().enhetNr
    }
}
