package mottak.behandlingsflyt

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.Journalpost
import mottak.http.HttpClientFactory
import java.time.LocalDate

interface Behandlingsflyt {
    fun finnEllerOpprettSak(journalpost: Journalpost.MedIdent): String
    fun sendSøknad(sakId: String, journalpostId: Long, søknad: ByteArray)
}

class BehandlingsflytClient(config: Config) : Behandlingsflyt {
    private val httpClient = HttpClientFactory.default()
    private val host = config.behandlingsflyt.host

    override fun finnEllerOpprettSak(journalpost: Journalpost.MedIdent): String {
        val ident = when (journalpost.personident) {
            is Ident.Personident -> journalpost.personident.id
            is Ident.Aktørid -> error("AktørID skal være byttet ut med folkeregisteridentifikator på dette tidspunktet")
        }

        return runBlocking {
            httpClient.post("$host/sak") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth("token")
                setBody(FinnEllerOpprettSak(ident, LocalDate.now()))
            }.body()
        }
    }

    data class FinnEllerOpprettSak(
        val personident: String,
        val mottatt: LocalDate,
    )

    override fun sendSøknad(
        sakId: String,
        journalpostId: Long,
        søknad: ByteArray,
    ) {
        runBlocking {
            httpClient.post("$host/sak/{$sakId}/søknad") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth("token")
                setBody(Søknad(journalpostId, mapOf("søknad" to søknad)))
            }
        }
    }

    data class Søknad(
        val journalpostId: Long,
        val søknad: Map<Any, Any>,
    )
}
