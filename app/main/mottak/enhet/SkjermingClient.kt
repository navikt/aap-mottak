package mottak.enhet

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.http.HttpClientFactory
import mottak.http.tryInto
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import java.net.URI

interface Skjerming {
    fun isSkjermet(personident: Ident.Personident): Boolean
}

class SkjermingClient(private val config: Config) : Skjerming {
    private val httpClient = HttpClientFactory.default()
    private val host: URI = config.skjerming.host
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun isSkjermet(personident: Ident.Personident): Boolean {
        val body = SkjermingReq(personident.id)

        return runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.skjerming.scope)
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
