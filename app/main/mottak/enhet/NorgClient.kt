package mottak.enhet

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.getEnvVar
import mottak.http.HttpClientFactory
import mottak.http.tryInto
import mottak.pdl.PdlGradering
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

data class NorgConfig(
    val host: String = getEnvVar("NORG_HOST"),
)

class NorgClient(private val config: Config) {
    private val host: String = config.norg.host
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    fun hentArbeidsfordeling(geografiskOmraade: String, skjermet: Boolean, gradering: PdlGradering): List<ArbeidsfordelingDtoResponse> {
        val request = ArbeidsfordelingDtoRequest(
            geografiskOmraade = geografiskOmraade,
            skjermet = skjermet,
            diskresjonskode = when(gradering) {
                PdlGradering.STRENGT_FORTROLIG, PdlGradering.STRENGT_FORTROLIG_UTLAND -> "SPSF"
                PdlGradering.FORTROLIG -> "SPFO"
                PdlGradering.UGRADERT -> "ANY"
            }
        )
        return runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
            httpClient.post("$host/api/v1/arbeidsfordeling/enheter/bestmatch") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(token)
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
