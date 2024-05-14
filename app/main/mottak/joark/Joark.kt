package mottak.joark

import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

interface Joark {
    fun oppdaterJournalpost(journalpost: Journalpost.MedIdent, fagsakId: String)

    fun ferdigstillJournalpost(journalpost: Journalpost)
}

const val FORDELINGSOPPGAVE = "FDR"
const val JOURNALFORINGSOPPGAVE = "JFR"

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JoarkClient(private val config: Config) : Joark {
    private val httpClient = HttpClientFactory.default()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun oppdaterJournalpost(journalpost: Journalpost.MedIdent, fagsakId: String) {
        val ident = when (journalpost.personident) {
            is Ident.Personident -> journalpost.personident.id
            is Ident.Aktørid -> error("AktørID skal være byttet ut med folkeregisteridentifikator på dette tidspunktet")
        }

        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.joark.scope)
            val response =
                httpClient.put("${config.joark.host}/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(OppdaterJournalpostRequest(
                        journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET,
                        sak = JournalpostSak(
                            fagsakId = fagsakId
                        ),
                        bruker = JournalpostBruker(
                            id = ident
                        )
                    ))
                }
            if (response.status.isSuccess()) {
                SECURE_LOG.info("Oppdaterte ${journalpost.journalpostId}")
            } else {
                error("Feil mot Joark (${response.status}): ${response.bodyAsText()}")
            }
        }
    }

    override fun ferdigstillJournalpost(journalpost: Journalpost) {
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.joark.scope)
            val response =
                httpClient.patch("${config.joark.host}/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}/ferdigstill") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(FerdigstillRequest(journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET))
                }
            if (response.status.isSuccess()) {
                SECURE_LOG.info("Ferdigstilt journalpost ${journalpost.journalpostId}")
            } else {
                error("Feil mot Joark (${response.status}): ${response.bodyAsText()}")
            }
        }
    }
}

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)

data class OppdaterJournalpostRequest(
    val behandlingstema: String? = null,
    val journalfoerendeEnhet: String,
    val sak: JournalpostSak,
    val tema: String = "AAP",
    val bruker: JournalpostBruker
)

data class JournalpostSak(
    val sakstype: String = "FAGSAK",
    val fagsakId: String,
    val fagsaksystem: String = "KELVIN"
)

data class JournalpostBruker(
    val id: String,
    val idType: String = "FNR"
)
