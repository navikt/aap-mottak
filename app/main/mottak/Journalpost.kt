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
    private val mottattDato: LocalDate,
    private val dokumenter: List<Dokument> = emptyList()
) {
    fun harFortsattTilstandMottatt(): Boolean {
        SECURE_LOG.info("Filter på status: ${status == JournalpostStatus.MOTTATT}")
        return status == JournalpostStatus.MOTTATT
    }

    fun erSøknad(): Boolean {
        return dokumenter.any {
            it.brevkode == SKJEMANUMMER_SØKNAD
        }
    }

    fun mottattDato() = mottattDato

    fun finnOriginal(): Dokument? = dokumenter.find {
        it.variantFormat == "ORIGINAL"
    }

    data class UtenIdent(
        override val journalpostId: Long,
        override val journalførendeEnhet: NavEnhet?,
        private val status: JournalpostStatus,
        private val mottattDato: LocalDate,
        private val dokumenter: List<Dokument>
    ) : Journalpost(journalpostId, journalførendeEnhet, status, mottattDato, dokumenter)

    data class MedIdent(
        val personident: Ident,
        override val journalpostId: Long,
        override val journalførendeEnhet: NavEnhet?,
        private val status: JournalpostStatus,
        private val mottattDato: LocalDate,
        private val dokumenter: List<Dokument>
    ) : Journalpost(journalpostId, journalførendeEnhet, status, mottattDato, dokumenter)
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
    val variantFormat: String,
    val brevkode: String?
)
