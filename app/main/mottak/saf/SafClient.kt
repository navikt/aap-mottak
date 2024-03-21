package mottak.saf

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.*
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import java.util.*

interface Saf {
    fun hentJournalpost(journalpostId: Long): Journalpost
}

class SafClient(private val config: Config) : Saf {
    private val httpClient = HttpClientFactory.default()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun hentJournalpost(journalpostId: Long): Journalpost {
        val request = SafRequest.hentJournalpost(journalpostId)
        val response = runBlocking { graphqlQuery(request) }

        val journalpost: SafJournalpost = response.data?.journalpost
            ?: error("Fant ikke journalpost for $journalpostId")

        val ident = when (journalpost.bruker?.type) {
            BrukerIdType.AKTOERID -> Ident.Aktørid(journalpost.bruker.id!!)
            BrukerIdType.FNR -> Ident.Personident(journalpost.bruker.id!!)
            else -> null.also {
                SECURE_LOG.warn("mottok noe annet enn personnr: ${journalpost.bruker?.type}")
            }
        }

        return if (ident == null) {
            Journalpost.UtenIdent(
                journalpostId = journalpost.journalpostId,
                status = JournalpostStatus.UKJENT,
                skjemanummer = ""
            )
        } else {
            Journalpost.MedIdent(
                journalpostId = journalpost.journalpostId,
                personident = ident,
                status = JournalpostStatus.UKJENT,
                erPliktkort = false,
                skjemanummer = ""
            )
        }
    }

    private suspend fun graphqlQuery(query: SafRequest): SafRespons {
        val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
        val request = httpClient.post("${config.saf.host}/graphql") {
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