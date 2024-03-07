package mottak.enhet

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.http.HttpClientFactory
import mottak.http.tryInto
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

data class SkjermingConfig(
    val host: String,
)

class SkjermingClient(private val config: Config) {
    private val httpClient = HttpClientFactory.create()
    private val host: String = config.skjerming.host
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    fun isSkjermet(personident: String): Boolean {
        val body = SkjermingReq(personident)

        return runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
            val response = httpClient.post("$host/skjermet") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(token)
                setBody(body)
            }

            response.tryInto<Boolean>()
        }
    }
}

data class SkjermingReq(val personident: String)
