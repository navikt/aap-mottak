package mottak.joark

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.SECURE_LOG
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

interface JoarkClient {
    fun ferdigstill(journalpostId: Long)
}

class JoarkClientImpl(private val config: Config) : JoarkClient {
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun ferdigstill(journalpostId: Long) {
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.joark.scope)
            val response =
                httpClient.patch("${config.joark.baseUrl}/rest/journalpostapi/v1/journalpost/${journalpostId}/ferdigstill") {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(FerdigstillRequest("enhet"))
                }
            if (response.status.isSuccess()) {
                SECURE_LOG.info("Ferdigstilte $journalpostId")
            } else {
                error("Feil mot Joark (${response.status}): ${response.bodyAsText()}")
            }
        }
    }
}

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)
