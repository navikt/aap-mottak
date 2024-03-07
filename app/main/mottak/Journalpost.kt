package mottak


data class Journalpost(
    val erMeldekort: Boolean, // TODO Skjemakode = Ny skjemakode for meldekort
    val erPliktkort : Boolean, // (Kelvin)
    val personident: String,
    val status: JournalpostStatus,
    val bruker: String?,
    val skjemanummer: String,
)

enum class JournalpostStatus {
    MOTTATT,
    JOURNALFØRT,
    UKJENT
}