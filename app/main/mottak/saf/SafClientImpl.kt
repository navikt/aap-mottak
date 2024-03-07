package mottak.saf

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Journalpost
import mottak.JournalpostStatus
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import java.util.UUID

interface SafClient {
    fun hentJournalpost(journalpostId: String): Journalpost
}

class SafClientImpl(private val config: Config) : SafClient {
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun hentJournalpost(journalpostId: String): Journalpost {
        val journalpost = runBlocking {
            graphqlQuery(SafRequest.hentJournalpost(journalpostId)).data?.journalpostById
                ?: throw Exception("Fant ikke journalpost for $journalpostId")
        }

        return Journalpost(
            journalpostId = "",
            erMeldekort = false,
            personident = "",
            status = JournalpostStatus.UKJENT,
            bruker = null,
            erPliktkort = false,
            skjemanummer = ""
        )
    }

    private suspend fun graphqlQuery(query: SafRequest): SafRespons {
        val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
        val request = httpClient.post("${config.saf.baseUrl}/graphql") {
            accept(ContentType.Application.Json)
            header("Nav-Callid", UUID.randomUUID().toString())
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(query)
        }

        val respons = request.body<SafRespons>()
        if (respons.errors != null) {
            throw Exception("Feil mot SAF: ${respons.errors}")
        }
        return respons
    }
}