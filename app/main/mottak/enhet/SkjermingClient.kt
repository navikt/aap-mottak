package mottak.enhet

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.http.HttpClientFactory
import mottak.http.tryInto

data class SkjermingConfig(
    val host: String,
)

class SkjermingClient(config: Config) {
    private val httpClient = HttpClientFactory.create()
    private val host: String = config.skjerming.host

    fun isSkjermet(ident: Ident): Boolean {
        val personident = when (ident) {
            is Ident.Personident -> ident.id
            is Ident.Aktørid -> ident.id    // todo: støtter skjerming aktørid?
        }

        val body = SkjermingReq(personident)

        return runBlocking {
            val response = httpClient.post("$host/skjermet") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(body)
            }

            response.tryInto<Boolean>()
        }
    }
}

data class SkjermingReq(val personident: String)
