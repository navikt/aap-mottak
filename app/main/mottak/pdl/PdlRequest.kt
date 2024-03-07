package mottak.pdl

import mottak.graphql.asQuery

internal data class PdlRequest(val query: String, val variables: Variables) {
    data class Variables(val ident: String? = null)

    companion object {
        fun hentPerson(personident: String) = PdlRequest(
            query = person.asQuery(),
            variables = Variables(ident = personident),
        )
    }
}

private const val ident = "\$ident"

private val person = """
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            foedsel {
                foedselsdato
            },
            adressebeskyttelse {
                gradering
            },
            bostedsadresse {
                vegadresse {
                    adressenavn
                    husbokstav
                    husnummer
                    postnummer
                }
            }
            navn {
                fornavn,
                etternavn,
                mellomnavn
            }
        }
    }
""".trimIndent()
