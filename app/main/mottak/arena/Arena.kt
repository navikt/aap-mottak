package mottak.arena

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

interface Arena {
    fun finnesSak(journalpost: Journalpost.MedIdent): Boolean
    fun opprettOppgave(journalpost: Journalpost.MedIdent)
}

class ArenaClient(private val config: Config) : Arena {
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun finnesSak(journalpost: Journalpost.MedIdent): Boolean {
        return runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.fssProxy.scope)
            val ident = when (journalpost.personident) {
                is Ident.Aktørid -> error("AktørID er ikke støttet i Oppgave")
                is Ident.Personident -> journalpost.personident.id
            }

            val response = httpClient.get("${config.fssProxy.host}/vedtak") {
                accept(ContentType.Application.Json)
                header("personident", ident)
                bearerAuth(token)
            }

            when {
                response.status.isSuccess() -> true
                response.status == HttpStatusCode.NotFound -> false
                else -> error("Feil mot arena (${response.status}): ${response.bodyAsText()}")
            }
        }
    }

    override fun opprettOppgave(journalpost: Journalpost.MedIdent) {
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.fssProxy.scope)
            val ident = when (journalpost.personident) {
                is Ident.Aktørid -> error("AktørID er ikke støttet i Oppgave")
                is Ident.Personident -> journalpost.personident.id
            }
            val response = httpClient.post("${config.fssProxy.host}/opprettoppgave") {
                accept(ContentType.Application.Json)
                header("personident", journalpost.personident)
                bearerAuth(token)
                setBody(
                    ArenaOpprettOppgaveParams(
                        fnr = Fødselsnummer(ident),
                        enhet = "",
                        tittel = "Tittel på journalpost",
                        titler = listOf("Vedleggstitler")
                    )
                )
            }
            if (response.status.isSuccess()) {
                SECURE_LOG.info("Opprettet oppgave i Arena for ${journalpost.personident}")
            } else {
                error("Feil mot arena (${response.status}): ${response.bodyAsText()}")
            }
        }
    }
}

data class ArenaOpprettOppgaveParams(
    val fnr: Fødselsnummer,
    val enhet: String,
    val tittel: String,
    val titler: List<String> = emptyList(),
)

data class Fødselsnummer(val fnr: String)
