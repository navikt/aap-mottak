package mottak.behandlingsflyt

import java.time.LocalDate

data class FinnEllerOpprettSak(
    val ident: String,
    val søknadsdato: LocalDate,
)

data class SendSøknad(
    val saksnummer: String,
    val journalpostId: String,
    val søknad: Temp = Temp()
)

data class Temp(
    val student: Boolean = true
)
