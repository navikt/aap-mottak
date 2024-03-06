package mottak


data class Journalpost(
    val erMeldekort: Boolean,
    val personident: String,
    val status: JournalpostStatus,
    val bruker: String?,
)

enum class JournalpostStatus {
    MOTTATT,
    JOURNALFØRT,
    UKJENT
}