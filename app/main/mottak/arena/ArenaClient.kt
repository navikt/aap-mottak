package mottak.arena

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

interface ArenaClient {
    fun sakFinnes(journalpost: Journalpost): Boolean
    fun opprettOppgave(journalpost: Journalpost)
}

class ArenaClientImpl(private val config: Config): ArenaClient {
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun sakFinnes(journalpost: Journalpost): Boolean {
        return runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.fssProxy.scope)
            val response = httpClient.get("${config.fssProxy.baseUrl}/vedtak") {
                accept(ContentType.Application.Json)
                header("personident", journalpost.personident)
                bearerAuth(token)
            }
            if (response.status.isSuccess()) {
                true
            } else if (response.status.value == 404){
                false
            } else {
                error("Feil mot arena (${response.status}): ${response.bodyAsText()}")
            }
        }
    }

    override fun opprettOppgave(journalpost: Journalpost) {
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.fssProxy.scope)
            val response = httpClient.post("${config.fssProxy.baseUrl}/opprettoppgave") {
                accept(ContentType.Application.Json)
                header("personident", journalpost.personident)
                bearerAuth(token)
                setBody(ArenaOpprettOppgaveParams(
                    fnr = Fødselsnummer(journalpost.personident),
                    enhet = "",
                    tittel = "Tittel på journalpost",
                    titler = listOf("Vedleggstitler")
                ))
            }
            if (response.status.isSuccess()) {
                SECURE_LOG.info("Opprettet oppgave i Arena for ${journalpost.personident}")
            } else {
                error("Feil mot arena (${response.status}): ${response.bodyAsText()}")
            }
        }
    }
}

data class ArenaOpprettOppgaveParams(val fnr : Fødselsnummer, val enhet : String, val tittel : String, val titler : List<String> = emptyList())

data class Fødselsnummer(val fnr : String)
