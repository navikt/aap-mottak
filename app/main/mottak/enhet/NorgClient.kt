package mottak.enhet

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.http.HttpClientFactory
import mottak.http.tryInto
import mottak.pdl.PdlGradering
import java.net.URI

interface Norg {
    fun hentArbeidsfordeling(
        geografiskOmraade: String,
        skjermet: Boolean,
        gradering: PdlGradering
    ): List<ArbeidsfordelingDtoResponse>
}

class NorgClient(config: Config) : Norg {
    private val host: URI = config.norg.host
    private val httpClient = HttpClientFactory.default()

    override fun hentArbeidsfordeling(
        geografiskOmraade: String,
        skjermet: Boolean,
        gradering: PdlGradering
    ): List<ArbeidsfordelingDtoResponse> {
        val request = ArbeidsfordelingDtoRequest(
            geografiskOmraade = geografiskOmraade,
            skjermet = skjermet,
            diskresjonskode = when (gradering) {
                PdlGradering.STRENGT_FORTROLIG, PdlGradering.STRENGT_FORTROLIG_UTLAND -> "SPSF"
                PdlGradering.FORTROLIG -> "SPFO"
                PdlGradering.UGRADERT -> "ANY"
            }
        )
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
