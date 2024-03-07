package mottak.pdl

import com.fasterxml.jackson.annotation.JsonProperty
import mottak.graphql.GraphQLError
import mottak.graphql.GraphQLExtensions
import java.time.LocalDate

internal data class PdlResponse(
    val data: PdlData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

internal data class PdlData(
    val hentPerson: PdlPerson?,
)

internal data class PdlPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>?,
    val navn: List<PdlNavn>?,
    val bostedsadresse: List<PdlBostedsadresse>? = null,
    val foedsel: List<PdlFoedsel>?,
    val forelderBarnRelasjon: List<PdlForelderBarnRelasjon>? = null,
    val fnr: String? = null,
    val doedsfall: Set<PDLDødsfall>? = null,
    val code: Code?     //Denne er påkrevd ved hentPersonBolk
)

internal data class PDLDødsfall(
    @JsonProperty("doedsdato") val dødsdato: LocalDate
)

internal enum class Code {
    ok, not_found, bad_request //TODO: add more
}

internal data class PdlForelderBarnRelasjon(
    val relatertPersonsIdent: String?
)

internal data class PdlFoedsel(
    val foedselsdato: String?
)

internal data class PdlBostedsadresse(
    val vegadresse: PdlVegadresse?
)

internal data class PdlVegadresse(
    val adressenavn: String,
    val husbokstav: String?,
    val husnummer: String?,
    val postnummer: String,
)

internal data class Adressebeskyttelse(
    val gradering: String
)

internal data class PdlNavn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?
)
