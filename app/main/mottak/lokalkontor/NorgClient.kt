package mottak.lokalkontor

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.getEnvVar
import mottak.http.HttpClientFactory
import mottak.http.tryInto

data class NorgConfig(
    val host: String = getEnvVar("NORG_HOST"),
)

class NorgClient(config: Config) {
    private val host: String = config.norg.host
    private val httpClient = HttpClientFactory.create()

    fun hentArbeidsfordeling(request: ArbeidsfordelingDtoRequest): List<ArbeidsfordelingDtoResponse> {
        return runBlocking {
            httpClient.post("$host/api/v1/arbeidsfordeling/enheter/bestmatch") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.tryInto()
        }
    }
}

object Behandlingstema {
    const val ARBEIDSAVKLARINGSPENGER = "ab0014"
}

data class ArbeidsfordelingDtoRequest(
    val geografiskOmraade: String,
    val skjermet: Boolean,
    val diskresjonskode: String,
    val tema: String = "AAP",
    val behandlingstema: String = Behandlingstema.ARBEIDSAVKLARINGSPENGER,
)

data class ArbeidsfordelingDtoResponse(
    val enhetNr: String,
)