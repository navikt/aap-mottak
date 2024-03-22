package mottak

import mottak.enhet.NavEnhet
import java.time.LocalDate

const val SKJEMANUMMER_SØKNAD = "NAV 11-13.05" // automatisk
const val SKJEMANUMMER_SØKNAD_ETTERSENDING = "NAVe 11-13.05" // automatisk
const val SKJEMANUMMER_PLIKTKORT = "TODO"
// 11-12.05 reisestønad automatisk
// e11-12.05 ettersendelse reisestønad automatisk

sealed class Journalpost(
    open val journalpostId: Long,
    private val status: JournalpostStatus,
    private val skjemanummer: String,
    private val mottattDato: LocalDate
) {
    fun erJournalført(): Boolean {
        return status == JournalpostStatus.JOURNALFØRT
    }

    fun erSøknad(): Boolean {
        return skjemanummer in listOf(
            SKJEMANUMMER_SØKNAD,
        )
    }

    fun mottattDato() = mottattDato

    class UtenIdent(
        journalpostId: Long,
        status: JournalpostStatus,
        skjemanummer: String,
        mottattDato: LocalDate
    ) : Journalpost(journalpostId, status, skjemanummer, mottattDato)

    data class MedIdent(
        val personident: Ident,
        val journalførendeEnhet: NavEnhet?,
        override val journalpostId: Long,
        private val erPliktkort: Boolean,
        private val skjemanummer: String,
        private val status: JournalpostStatus,
        private val mottattDato: LocalDate
    ) : Journalpost(journalpostId, status, skjemanummer, mottattDato) {

        fun erEttersending(): Boolean {
            return skjemanummer == SKJEMANUMMER_SØKNAD_ETTERSENDING
        }

        fun erPliktkort(): Boolean {
            return skjemanummer == SKJEMANUMMER_PLIKTKORT
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
