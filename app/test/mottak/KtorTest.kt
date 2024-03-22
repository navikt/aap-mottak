package mottak

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import lib.kafka.StreamsMock
import mottak.kafka.Topics
import mottak.pdl.PdlResponse
import mottak.saf.*
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension

@ExtendWith(SystemStubsExtension::class)
class KtorTest {

    @SystemStub
    val naisCluster = EnvironmentVariables("NAIS_CLUSTER_NAME", "dev")

    @Test
    fun `Test hele greia`() {
        val kafka = StreamsMock()
        val config = TestConfig()
        val topics = Topics(config.kafka)

        testApplication {
            application {
                server(config, kafka)
            }
            externalServices {
                hosts(config.pdl.host.host, block = Application::pdlFake)
                hosts(config.saf.host.host, block = Application::safFake)
            }
        }

        val journalføringstopic = kafka.testTopic(topics.journalfoering)
        journalføringstopic.produce("1") {
            JournalfoeringHendelseRecord.newBuilder().apply {
                hendelsesId = "1"
                versjon = 1
                hendelsesType = ""
                journalpostId = 123L
                temaGammelt = "AAP"
                temaNytt = "AAP"
                journalpostStatus = "MOTTATT"
                mottaksKanal = "NAV_NO"
                kanalReferanseId = ""
                behandlingstema = ""
            }.build()
        }

        // todo: fake and assert
    }
}

internal fun Application.safFake() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/graphql") {
            call.parameters["Nav-Call-Id"] ?: error("missing call id")
            call.parameters["Authotization"] ?: error("missing token")
            call.respond(
                SafRespons(
                    data = SafData(
                        journalpost = SafJournalpost(
                            journalpostId = 123,
                            journalposttype = Journalposttype.I,
                            relevanteDatoer = emptyList(),
                            bruker = Bruker("123", BrukerIdType.FNR),
                            sak = Sak(
                                fagsaksystem = "Kelvin",
                                sakstype = Sakstype.FAGSAK,
                            )
                        )
                    )
                ),
            )
        }
    }
}

internal fun Application.pdlFake() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/graphql") {
            call.parameters["Nav-Call-Id"] ?: error("missing call id")
            call.parameters["TEMA"] == "AAP" || error("missing tema AAP")
            call.parameters["Authotization"] ?: error("missing token")
            call.respond(
                PdlResponse(
                    data = PdlResponse.Data(
                        hentGeografiskTilknytning = PdlResponse.Data.GeografiskTilknytning(
                            gtType = PdlResponse.Data.GeografiskTilknytning.Type.KOMMUNE,
                            gtKommune = "0301",
                            gtLand = null,
                            gtBydel = null,
                        ),
                        hentPerson = PdlResponse.Data.Person(
                            folkeregisteridentifikator = listOf(
                                PdlResponse.Data.FolkeregisterIdentifikator("123"),
                            ),
                            adressebeskyttelse = listOf(
                                PdlResponse.Data.Adressebeskyttelse(
                                    gradering = "UGRADERT"
                                )
                            ),
                            navn = listOf(
                                PdlResponse.Data.Navn(
                                    fornavn = "fornavn",
                                    mellomnavn = "mellomnavn",
                                    etternavn = "etternavn",
                                )
                            )
                        ),
                    ),
                    errors = emptyList()
                )
            )
        }
    }
}
