package mottak.lokalkontor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.SECURE_LOG

data class SkjermingConfig(
    val host: String,
)

class SkjermingClient(config: Config) {
    private val httpClient = HttpClient(CIO) // TODO: bytt ut med http client factory
    private val host: String = config.skjerming.host

    fun isSkjermet(personident: String): Boolean {
        val body = SkjermingReq(personident)

        return runBlocking {
            val response = httpClient.post("$host/skjermet") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(body)
            }

            when (response.status.value) {
                in 200..299 -> response.body<Boolean>()
                in 400..499 -> throw response.logWithError("Client error calling nom")
                in 500..599 -> throw response.logWithError("Server error calling nom")
                else -> throw response.logWithError("Unknown error calling nom")
            }
        }
    }
}

fun HttpResponse.logWithError(msg: String): IllegalStateException {
    SECURE_LOG.error(
        """
            $msg
            Response: $status
            Headers: $headers
            Body: ${runBlocking { bodyAsText() }}
        """.trimIndent()
    )

    return IllegalStateException(
        """
        $msg
        Response: $status
        Headers: $headers
    """.trimIndent()
    )
}

data class SkjermingReq(val personident: String)
