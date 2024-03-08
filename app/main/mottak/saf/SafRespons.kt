package mottak.saf

import mottak.graphql.GraphQLError
import mottak.graphql.GraphQLExtensions
import java.time.LocalDateTime

data class SafRespons(
    val data: SafData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class SafData(
    val dokumentoversiktSelvbetjening: SafDokumentoversikt?,
    val journalpostById: SafJournalpost?
)

data class SafDokumentoversikt(
    val journalposter: List<SafJournalpost?>
)

data class SafJournalpost(
    val journalpostId: String,
    val journalposttype: String,
    val eksternReferanseId: String?,
    val tittel: String?,
    val bruker: Ident?,
    val relevanteDatoer: List<SafRelevantDato>,
    val dokumenter: List<SafDokumentInfo?>?
) {

    data class Ident(
        val id: String,
        val type: IdType = IdType.UKJENT,
    ) {
        enum class IdType {
            FNR,
            AKTOERID,
            UKJENT
        }
    }
}


data class SafRelevantDato(
    val dato: LocalDateTime,
    val datotype: SafDatoType
)

enum class SafDatoType {
    DATO_OPPRETTET, DATO_SENDT_PRINT, DATO_EKSPEDERT,
    DATO_JOURNALFOERT, DATO_REGISTRERT,
    DATO_AVS_RETUR, DATO_DOKUMENT
}

data class SafDokumentInfo(
    val dokumentInfoId: String,
    val brevkode: String?,
    val tittel: String?,
    val dokumentvarianter: List<SafDokumentvariant?>
)

data class SafDokumentvariant(
    val variantformat: SafVariantformat,
    val brukerHarTilgang: Boolean,
    val filtype: String
)

enum class SafVariantformat {
    ARKIV, SLADDET, ORIGINAL
}

