package mottak.saf

import mottak.graphql.asQuery

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(val journalpostId: Long? = null)

    companion object {
        fun hentJournalpost(journalpostId: Long) = SafRequest(
            query = journalpost.asQuery(),
            variables = Variables(journalpostId = journalpostId)
        )
    }
}

private const val journalpostId = "\$journalpostId"

private val journalpost = """
    query($journalpostId: String!) {
        journalpostById(journalpostId: $journalpostId) {
            journalpostId
            tittel
            journalposttype
            eksternReferanseId
            relevanteDatoer {
                dato
                datotype
            }
            avsender {
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

