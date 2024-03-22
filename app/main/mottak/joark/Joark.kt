package mottak.joark

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.enhet.NavEnhet
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

interface Joark {
    fun oppdaterJournalpost(journalpost: Journalpost, enhet: NavEnhet, fagsakId: String)

    fun ferdigstillJournalpost(journalpost: Journalpost, enhet: NavEnhet)
}

const val FORDELINGSOPPGAVE = "FDR"
const val JOURNALORINGSOPPGAVE = "JFR"

class JoarkClient(private val config: Config) : Joark {
    private val httpClient = HttpClientFactory.default()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun oppdaterJournalpost(journalpost: Journalpost, enhet: NavEnhet, fagsakId: String) {
        // TODO: Oppdater med behandlende enhet og fagsak
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.joark.scope)
            val response =
                httpClient.put("${config.joark.host}/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}") {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(OppdaterJournalpostRequest(
                        journalfoerendeEnhet = enhet.nr,
                        sak = JournalpostSak(
                            fagsakId = fagsakId
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

    override fun ferdigstillJournalpost(journalpost: Journalpost, enhet: NavEnhet) {
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.joark.scope)
            val response =
                httpClient.put("${config.joark.host}/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}/ferdigstill") {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(FerdigstillRequest(journalfoerendeEnhet = enhet.nr))
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
    val sak: JournalpostSak
)

data class JournalpostSak(
    val sakstype: String = "FAGSAK",
    val fagsakId: String,
    val fagsaksystem: String = "KELVIN"
)