package mottak.gosys

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.arena.ArenaOpprettOppgaveParams
import mottak.arena.Fødselsnummer
import mottak.enhet.NavEnhet
import mottak.http.HttpClientFactory
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider

interface Gosys {
    fun opprettManuellJournalføringsoppgave(journalpost: Journalpost.MedIdent)
    fun opprettOppgaveForManglendeIdent(journalpost: Journalpost.UtenIdent)
    fun opprettAutomatiskJournalføringsoppgave(journalpost: Journalpost.MedIdent, enhet: NavEnhet)
}

class GosysClient(private val config: Config) : Gosys {
    private val httpClient = HttpClientFactory.create()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    override fun opprettManuellJournalføringsoppgave(journalpost: Journalpost.MedIdent) {
        runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
            val ident = when (journalpost.personident) {
                is Ident.Aktørid -> error("AktørID er ikke støttet i Oppgave")
                is Ident.Personident -> journalpost.personident.id
            }

            val response = httpClient.post("${config.gosys.host}/opprettoppgave") {
                accept(ContentType.Application.Json)
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
                SECURE_LOG.info("Opprettet oppgave i Oppgave for ${journalpost.personident}")
            } else {
                error("Feil mot Oppgave (${response.status}): ${response.bodyAsText()}")
            }
        }
    }

    override fun opprettOppgaveForManglendeIdent(journalpost: Journalpost.UtenIdent) {
        SECURE_LOG.info("Scanning har ikke klart å lese bruker, manuell behandling")
    }

    override fun opprettAutomatiskJournalføringsoppgave(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        TODO("Not yet implemented")
    }
}
