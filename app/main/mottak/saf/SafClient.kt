package mottak.saf

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.*
import mottak.enhet.NavEnhet
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import java.util.*

interface Saf {
    fun hentJournalpost(journalpostId: Long): Journalpost
    fun hentJson(journalpostId: Long): ByteArray?
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

        val mottattDato = journalpost.relevanteDatoer?.find { dato ->
            dato?.datotype == SafDatoType.DATO_REGISTRERT
        }?.dato?.toLocalDate() ?: error("Fant ikke dato")

        val dokumenter = journalpost.dokumenter?.filterNotNull()?.flatMap { dokument ->
            dokument.dokumentvarianter.filterNotNull().map { variant ->
                Dokument(dokument.dokumentInfoId, variant.variantformat.name, dokument.brevkode)
            }
        } ?: emptyList()

        return if (ident == null) {
            Journalpost.UtenIdent(
                journalpostId = journalpost.journalpostId,
                status = JournalpostStatus.UKJENT,
                journalførendeEnhet = journalpost.journalfoerendeEnhet?.let(::NavEnhet),
                mottattDato = mottattDato,
                dokumenter = dokumenter
            )
        } else {
            Journalpost.MedIdent(
                journalpostId = journalpost.journalpostId,
                personident = ident,
                status = JournalpostStatus.UKJENT,
                journalførendeEnhet = journalpost.journalfoerendeEnhet?.let(::NavEnhet),
                mottattDato = mottattDato,
                dokumenter = dokumenter
            )
        }
    }

    override fun hentJson(journalpostId: Long): ByteArray? {
        val journalpost = hentJournalpost(journalpostId)
        val origialDokument = journalpost.finnOriginal()
        return origialDokument?.let {
            runBlocking {
                restQuery(journalpostId, it.dokumentInfoId)
            }
        }
    }

    private suspend fun graphqlQuery(query: SafRequest): SafRespons {
        val token = tokenProvider.getClientCredentialToken(config.saf.scope)
        val request = httpClient.post("${config.saf.host}/graphql") {
            accept(ContentType.Application.Json)
            header("Nav-Callid", UUID.randomUUID().toString())
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(query)
        }

        val respons = request.body<SafRespons>()

        if (respons.hasErrors()) {
            error("Feil mot SAF: ${respons.errors}")
        }

        return respons
    }

    private suspend fun restQuery(
        journalpostId: Long,
        dokumentId: String,
        arkivtype: String = "ARKIV"
    ): ByteArray {
        val token = tokenProvider.getClientCredentialToken(config.saf.scope)
        val response = httpClient.get("${config.saf.host}/rest/hentdokument/$journalpostId/$dokumentId/$arkivtype") {
            header("Nav-Call-Id", UUID.randomUUID().toString())
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }
        return when(response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> error("Fant ikke dokument $dokumentId for journalpost $journalpostId")
            else -> error("Feil fra saf: ${response.status} : ${response.bodyAsText()}")
        }
    }
}