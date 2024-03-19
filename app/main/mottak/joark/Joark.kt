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
    fun oppdaterJournalpost(journalpost: Journalpost, enhet: NavEnhet)
}

const val FORDELINGSOPPGAVE = "FDR"
const val JOURNALORINGSOPPGAVE = "JFR"

class JoarkClient(private val config: Config) : Joark {
    private val httpClient = HttpClientFactory.default()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun oppdaterJournalpost(journalpost: Journalpost, enhet: NavEnhet) {
        // TODO: Oppdater med behandlende enhet og fagsak
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.joark.scope)
            val response =
                httpClient.patch("${config.joark.host}/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}/ferdigstill") {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(FerdigstillRequest(enhet.nr))
                }
            if (response.status.isSuccess()) {
                SECURE_LOG.info("Ferdigstilte ${journalpost.journalpostId}")
            } else {
                error("Feil mot Joark (${response.status}): ${response.bodyAsText()}")
            }
        }
    }
}

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)
