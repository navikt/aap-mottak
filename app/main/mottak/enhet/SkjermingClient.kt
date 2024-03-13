package mottak.enhet

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.http.HttpClientFactory
import mottak.http.tryInto

interface Skjerming {
    fun isSkjermet(personident: Ident.Personident): Boolean
}

class SkjermingClient(config: Config) : Skjerming {
    private val httpClient = HttpClientFactory.create()
    private val host: String = config.skjerming.host

    override fun isSkjermet(personident: Ident.Personident): Boolean {
        val body = SkjermingReq(personident.id)

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
