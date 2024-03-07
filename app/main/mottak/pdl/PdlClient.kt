package mottak.pdl

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

class PdlGraphQLClient(private val config: Config) {
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    fun hentPerson(personident: String): String {
        val res = runBlocking {
            query(PdlRequest.hentPerson(personident), "")
        }
        return ""
    }

    private suspend fun query(query: PdlRequest, callId: String): Result<PdlResponse> {
        val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
        val request = httpClient.post(config.pdl.baseUrl) {
            accept(ContentType.Application.Json)
            header("Nav-Call-Id", callId)
            header("TEMA", "AAP")
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(query)
        }
        return runCatching {
            val respons = request.body<PdlResponse>()
            if (respons.errors != null) {
                throw Exception("Feil mot PDL: ${respons.errors}")
            }
            respons
        }
    }
}
