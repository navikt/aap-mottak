package mottak.pdl

data class PdlRequest(val query: String, val variables: Variables) {
    data class Variables(val ident: String)

    companion object {
        fun hentPersonopplysninger(personident: String) = PdlRequest(
            query = personopplysninger.replace("\n", ""),
            variables = Variables(personident),
        )
    }
}

// escape $ in multiline strings
private const val ident = "\$ident"

private const val personopplysninger = """
    query($ident: ID!) {
        hentGeografiskTilknytning(ident: $ident) {
            gtKommune
            gtBydel
            gtLand
            gtType
        }
        hentPerson(ident: $ident) {
            adressebeskyttelse {
                gradering
            }
            folkeregisteridentifikator {
                identifikasjonsnummer
            }
        }
    }
"""
