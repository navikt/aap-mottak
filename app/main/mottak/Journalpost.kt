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
    open val journalførendeEnhet: NavEnhet?,
    private val status: JournalpostStatus,
    private val skjemanummer: String,
    private val mottattDato: LocalDate,
    private val dokumenter: List<Dokument> = emptyList()
) {
    fun skjemanummer() = skjemanummer

    fun erJournalført(): Boolean {
        return status == JournalpostStatus.JOURNALFØRT
    }

    fun erSøknad(): Boolean {
        return skjemanummer in listOf(
            SKJEMANUMMER_SØKNAD,
        )
    }

    fun mottattDato() = mottattDato

    fun finnOriginal(): Dokument? = dokumenter.find {
        it.variantFormat == "ORIGINAL"
    }

    data class UtenIdent(
        override val journalpostId: Long,
        override val journalførendeEnhet: NavEnhet?,
        private val status: JournalpostStatus,
        private val skjemanummer: String,
        private val mottattDato: LocalDate
    ) : Journalpost(journalpostId, journalførendeEnhet, status, skjemanummer, mottattDato)

    data class MedIdent(
        val personident: Ident,
        override val journalpostId: Long,
        override val journalførendeEnhet: NavEnhet?,
        private val status: JournalpostStatus,
        private val skjemanummer: String,
        private val mottattDato: LocalDate,
        private val dokumenter: List<Dokument>
    ) : Journalpost(journalpostId, journalførendeEnhet, status, skjemanummer, mottattDato, dokumenter)
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

data class Dokument(
    val dokumentInfoId: String,
    val variantFormat: String
)
