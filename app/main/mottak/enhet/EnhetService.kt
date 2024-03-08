package mottak.enhet

import mottak.Config
import mottak.pdl.Personopplysninger

class EnhetService(config: Config) {
    private val norg = NorgClient(config)
    private val skjerming = SkjermingClient(config)

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
