package mottak.gosys

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.arena.ArenaOpprettOppgaveParams
import mottak.arena.Fødselsnummer
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

interface GosysClient {
    fun opprettOppgave(journalpost: Journalpost)
    fun opprettOppgaveForManglendeIdent(journalpost: Journalpost)
}

class GosysClientImpl(private val config: Config): GosysClient {
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun opprettOppgave(journalpost: Journalpost) {
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
            val response = httpClient.post("${config.gosys.baseUrl}/opprettoppgave") {
                accept(ContentType.Application.Json)
                bearerAuth(token)
                setBody(ArenaOpprettOppgaveParams(
                    fnr = Fødselsnummer(journalpost.personident),
                    enhet = "",
                    tittel = "Tittel på journalpost",
                    titler = listOf("Vedleggstitler")
                ))
            }
            if (response.status.isSuccess()) {
                SECURE_LOG.info("Opprettet oppgave i Oppgave for ${journalpost.personident}")
            } else {
                error("Feil mot Oppgave (${response.status}): ${response.bodyAsText()}")
            }
        }
    }

    override fun opprettOppgaveForManglendeIdent(journalpost: Journalpost) {
        SECURE_LOG.info("Scanning har ikke klart å lese bruker, manuell behandling")
    }
}
