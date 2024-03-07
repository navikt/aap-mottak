package mottak.lokalkontor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import mottak.Config
import java.net.URI
import java.util.*

data class PdlConfig(
    val url: URI,
    val scope: String,
    val audience: String,
)

class PdlClient(private val config: Config) {
    private val httpClient = HttpClient(CIO) // todo bytt med http client factory

    suspend fun hentGTogGradering(personident: String): PdlResponse {
        val query = PdlRequest.hentGtOgGradering(personident)
        return fetch(query)
    }

    private suspend fun fetch(query: PdlRequest): PdlResponse {
        val request = httpClient.post(config.pdl.url.toURL()) {
            accept(ContentType.Application.Json)
            header("Nav-Call-Id", UUID.randomUUID())
            header("TEMA", "AAP")
//            bearerAuth(azure.getClientCredential())
            contentType(ContentType.Application.Json)
            setBody(query)
        }

        when (request.status.value) {
            in 200..299 -> return request.body()
            in 400..499 -> error("Client error calling PDL: ${request.status}")
            in 500..599 -> error("Server error calling PDL: ${request.status}")
            else -> error("Unknown error calling PDL: ${request.status}")
        }
    }
}

data class PdlRequest(val query: String, val variables: Variables) {
    data class Variables(val ident: String)

    companion object {
        fun hentGtOgGradering(personident: String) = PdlRequest(
            query = gtOgGradering.replace("\n", ""),
            variables = Variables(personident),
        )
    }
}

// escape $ in multiline strings
private const val ident = "\$ident"

private const val gtOgGradering = """
    query($ident: ID!) {
        hentGeografiskTilknytning(ident: $ident) {
            gtKommune
            gtBydel
            gtLand
        }
        hentPerson(ident: $ident) {
            adressebeskyttelse {
                gradering
            }
        }
    }
"""

data class PdlResponse(
    val data: Data?,
    val errors: List<Error>?,
) {
    val adressebeskyttelse: Data.Adressebeskyttelse?
        get() = data
            ?.hentPerson
            ?.adressebeskyttelse
            ?.singleOrNull()

    val gradering: String
        get() = data
            ?.hentPerson
            ?.adressebeskyttelse
            ?.singleOrNull()
            ?.gradering
            ?: "UGRADERT"

    val geografiskTilknytning: String
        get() = data?.hentGeografiskTilknytning?.gtBydel
            ?: data?.hentGeografiskTilknytning?.gtKommune
            ?: data?.hentGeografiskTilknytning?.gtLand
            ?: data?.hentGeografiskTilknytning?.gtType
            ?: "UKJENT"

    data class Data(
        val hentGeografiskTilknytning: GeografiskTilknytning?,
        val hentPerson: Person?,
    ) {
        data class Person(
            val adressebeskyttelse: List<Adressebeskyttelse>,
            val navn: List<Navn>,
        )

        data class Adressebeskyttelse(val gradering: String)

        data class GeografiskTilknytning(
            val gtLand: String?,
            val gtKommune: String?,
            val gtBydel: String?,
            val gtType: String
        )

        data class Navn(
            val fornavn: String,
            val etternavn: String,
            val mellomnavn: String?,
        )
    }

    data class Error(
        val message: String,
        val locations: List<Location>,
        val path: List<String>?,
        val extensions: Extensions
    ) {
        data class Location(val line: Int?, val column: Int?)
        data class Extensions(val code: String?, val classification: String)
    }
}
