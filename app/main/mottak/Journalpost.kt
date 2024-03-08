package mottak

const val SKJEMANUMMER_SØKNAD = "NAV 11-13.05"
const val SKJEMANUMMER_SØKNAD_ETTERSENDING = "NAVe 11-13.05"

sealed interface Journalpost {

    fun erJournalført(): Boolean
    fun erMeldekort(): Boolean
    fun erSøknadEllerEttersending(): Boolean

    data class MedIdent(
        val journalpostId: String,
        val erPliktkort: Boolean, // (Kelvin)
        val personident: Ident,
        private val erMeldekort: Boolean, // TODO Skjemakode = Ny skjemakode for meldekort
        private val status: JournalpostStatus,
        private val skjemanummer: String,
    ) : Journalpost {

        fun erEttersending(): Boolean {
            return skjemanummer == SKJEMANUMMER_SØKNAD_ETTERSENDING
        }

        override fun erJournalført(): Boolean {
            return status == JournalpostStatus.JOURNALFØRT
        }

        override fun erMeldekort(): Boolean {
            return erMeldekort
        }

        override fun erSøknadEllerEttersending(): Boolean {
            return skjemanummer in listOf(
                SKJEMANUMMER_SØKNAD,
                SKJEMANUMMER_SØKNAD_ETTERSENDING
            )
        }

    }

    class UtenIdent(
        val journalpostId: String,
        private val erMeldekort: Boolean, // TODO Skjemakode = Ny skjemakode for meldekort
        private val status: JournalpostStatus,
        private val skjemanummer: String,
    ) : Journalpost {
        override fun erJournalført(): Boolean {
            return status == JournalpostStatus.JOURNALFØRT
        }

        override fun erMeldekort(): Boolean {
            return erMeldekort
        }

        override fun erSøknadEllerEttersending(): Boolean {
            return skjemanummer in listOf(
                SKJEMANUMMER_SØKNAD,
                SKJEMANUMMER_SØKNAD_ETTERSENDING
            )
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
