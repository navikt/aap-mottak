package mottak

const val SKJEMANUMMER_SØKNAD = "NAV 11-13.05"
const val SKJEMANUMMER_SØKNAD_ETTERSENDING = "NAVe 11-13.05"

sealed class Journalpost(
    open val journalpostId: String,
    private val status: JournalpostStatus,
    private val skjemanummer: String,
) {
    fun erJournalført(): Boolean {
        return status == JournalpostStatus.JOURNALFØRT
    }

    fun erSøknadEllerEttersending(): Boolean {
        return skjemanummer in listOf(
            SKJEMANUMMER_SØKNAD,
            SKJEMANUMMER_SØKNAD_ETTERSENDING
        )
    }

    class UtenIdent(
        journalpostId: String,
        status: JournalpostStatus,
        skjemanummer: String,
    ) : Journalpost(journalpostId, status, skjemanummer)

    data class MedIdent(
        val personident: Ident,
        override val journalpostId: String,
        private val erPliktkort: Boolean,
        private val skjemanummer: String,
        private val status: JournalpostStatus,
    ) : Journalpost(journalpostId, status, skjemanummer) {

        fun erEttersending(): Boolean {
            return skjemanummer == SKJEMANUMMER_SØKNAD_ETTERSENDING
        }

        fun erPliktkort(): Boolean {
            return erPliktkort
        }

        fun erSøknad(): Boolean {
            return skjemanummer == SKJEMANUMMER_SØKNAD
        }
    }
}

sealed class Ident {
    class Personident(val id: String) : Ident()
    class Aktørid(val id: String) : Ident()
}

enum class JournalpostStatus {
    MOTTATT,
    JOURNALFØRT,
    UKJENT
}
