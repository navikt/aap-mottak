package mottak.pdl

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.http.HttpClientFactory
import mottak.http.tryInto
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import java.net.URI
import java.util.*

enum class PdlGradering {
    STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND, FORTROLIG, UGRADERT
}

data class PdlConfig(
    val host: URI,
    val scope: String,
    val audience: String,
)

class MissingPdlOpplysningException(msg: String) : RuntimeException(msg)

interface PdlClient {
    fun hentPersonopplysninger(ident: Ident): Personopplysninger
}

class PdlClientImpl(private val config: Config) : PdlClient {
    private val httpClient = HttpClientFactory.create()
    private val url = config.pdl.host.toURL()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun hentPersonopplysninger(ident: Ident): Personopplysninger {
        val personident = when (ident) {
            is Ident.Personident -> ident.id
            is Ident.Aktørid -> ident.id
        }

        val data = query(PdlRequest.hentPersonopplysninger(personident))

        return Personopplysninger(
            personident = Ident.Personident(data.folkeregisteridentifikator),
            gradering = PdlGradering.valueOf(data.gradering),
            gt = data.geografiskTilknytning
        )
    }

    private fun query(query: PdlRequest): PdlResponse.Data {
        return runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
            httpClient.post(url) {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", UUID.randomUUID())
                header("TEMA", "AAP")
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(query)
            }
                .tryInto<PdlResponse>()
                .takeDataOrThrow()
        }
    }
}

data class Personopplysninger(
    val personident: Ident.Personident,
    val gradering: PdlGradering,
    val gt: String
)

fun PdlResponse.takeDataOrThrow(): PdlResponse.Data {
    return when (errors.isNullOrEmpty()) {
        true -> data ?: error("missing data in pdl response")
        false -> error("pdl response has errors: $errors")
    }
}
