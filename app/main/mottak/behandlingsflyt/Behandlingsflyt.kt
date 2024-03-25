package mottak.behandlingsflyt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.Journalpost
import mottak.http.HttpClientFactory
import java.time.LocalDate

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

        return runBlocking { finnEllerOpprett(ident, journalpost.mottattDato()) }
    }

    private suspend fun finnEllerOpprett(ident: String, mottattDato: LocalDate): Saksinfo {
        val response = httpClient.post("$host/api/sak/finnEllerOpprett") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth("token")
                setBody(FinnEllerOpprettSak(ident, mottattDato))
            }

        return when(response.status) {
            HttpStatusCode.OK -> response.body()
            else -> error("Feil fra behandlingsflyt: ${response.status} : ${response.bodyAsText()}")
        }
    }

    override fun sendSøknad(
        sakId: String,
        journalpostId: Long,
        søknad: ByteArray,
    ) {
        val typeRef: TypeReference<Map<Any, Any>> = object : TypeReference<Map<Any, Any>>() {}
        val map = objectMapper.readValue(søknad, typeRef)
        runBlocking {
            httpClient.post("$host/api/søknad/send") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth("token")
                setBody(SendSøknad(sakId, journalpostId.toString(), map))
            }
        }
    }
}

private val objectMapper = ObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())
    .registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, true)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )
