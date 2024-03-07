package mottak

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import mottak.kafka.Topics
import mottak.kafka.createTopology
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.kafka.streams.v2.test.StreamsMock
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension

@ExtendWith(SystemStubsExtension::class)
class AppTest {

    @SystemStub
    val environmentVariables = EnvironmentVariables(mapOf(
        "KAFKA_SCHEMA_REGISTRY" to "mock://kafka",
        "KAFKA_SCHEMA_REGISTRY_USER" to "",
        "KAFKA_SCHEMA_REGISTRY_PASSWORD" to "",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to ""
    ))

    val joark = JoarkClientFake()
    val kelvin = BehandlingsflytClientFake()
    val arena = ArenaClientFake()
    val gosys = GosysClientFake()
    val saf = SafClientFake()

    val streamsconfig = StreamsConfig(
        applicationId = "",
        brokers = ""
    )

    @Test
    fun `Test hele greia`() {
        val kafka = StreamsMock()

        kafka.connect(
            topology = createTopology(saf, joark, kelvin, arena, gosys),
            config = streamsconfig,
            registry = SimpleMeterRegistry(),
        )

        val journalføringstopic = kafka.testTopic(Topics.journalfoering)
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

        Assertions.assertEquals(true, arena.harOpprettetOppgaveMedId("123"))
    }
}