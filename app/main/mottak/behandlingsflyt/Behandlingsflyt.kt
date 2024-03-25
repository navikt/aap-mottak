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

data class FinnEllerOpprettSak(
    val ident: String,
    val søknadsdato: LocalDate,
)

data class Saksinfo(
    val saksnummer: String,
    val periode: Periode
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)

interface Behandlingsflyt {
    fun finnEllerOpprettSak(journalpost: Journalpost.MedIdent): Saksinfo
    fun sendSøknad(sakId: String, journalpostId: Long, søknad: ByteArray)
}

class BehandlingsflytClient(config: Config) : Behandlingsflyt {
    private val httpClient = HttpClientFactory.default()
    private val host = config.behandlingsflyt.host

    override fun finnEllerOpprettSak(journalpost: Journalpost.MedIdent): Saksinfo {
        val ident = when (journalpost.personident) {
            is Ident.Personident -> journalpost.personident.id
            is Ident.Aktørid -> error("AktørID skal være byttet ut med folkeregisteridentifikator på dette tidspunktet")
        }

        return runBlocking {
            httpClient.post("$host/api/sak/finnEllerOpprett") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth("token")
                setBody(FinnEllerOpprettSak(ident, journalpost.mottattDato()))
            }.body()
        }
    }

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
        val søknad: Any,
    )
}
