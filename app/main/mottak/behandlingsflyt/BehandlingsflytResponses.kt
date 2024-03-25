package mottak.behandlingsflyt

import java.time.LocalDate

data class Saksinfo(
    val saksnummer: String,
    val periode: Periode
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)