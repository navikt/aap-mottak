package mottak.saf

import mottak.graphql.asQuery

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(val journalpostId: String? = null)

    companion object {
        fun hentJournalpost(journalpostId: String) = SafRequest(
            query = journalpost.asQuery(),
            variables = Variables(journalpostId = journalpostId)
        )
    }
}

private const val journalpostId = "\$journalpostId"

private val journalpost = """
    query journalpostById($journalpostId: String!) {
        journalpostById(journalpostId: $journalpostId) {
            journalpostId
            tittel
            journalposttype
            eksternReferanseId
            relevanteDatoer {
                dato
                datotype
            }
            bruker {
                id
                type
            }
            dokumenter {
                dokumentInfoId
                brevkode
                tittel
                dokumentvarianter {
                    variantformat
                    brukerHarTilgang
                    filtype
                }
            }
        } 
    }
""".trimIndent()

