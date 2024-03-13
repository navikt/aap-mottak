package mottak

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import mottak.enhet.EnhetService
import mottak.kafka.MottakTopology
import mottak.kafka.Topics
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.kafka.streams.v2.test.StreamsMock
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension

@ExtendWith(SystemStubsExtension::class)
class StreamsTest {

    @SystemStub
    val env = EnvironmentVariables(
        mapOf(
            "KAFKA_SCHEMA_REGISTRY" to "mock://kafka",
            "KAFKA_SCHEMA_REGISTRY_USER" to "",
            "KAFKA_SCHEMA_REGISTRY_PASSWORD" to "",
            "KAFKA_TRUSTSTORE_PATH" to "",
            "KAFKA_KEYSTORE_PATH" to "",
            "KAFKA_CREDSTORE_PASSWORD" to ""
        )
    )

    @Test
    fun `Test hele greia`() {
        val kafka = StreamsMock()
        val topology = MottakTopology(
            saf = SafFake,
            joark = JoarkFake,
            pdl = PdlFake,
            kelvin = BehandlingsflytFake,
            arena = ArenaFake,
            gosys = GosysFake,
            enhetService = EnhetService(
                norg = NorgFake,
                skjerming = SkjermingFake,
            )
        )

        kafka.connect(
            topology = topology(),
            config = StreamsConfig("", ""),
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

        assertTrue(GosysFake.harOpprettetOppgave("123", "oslo"))
        assertTrue(ArenaFake.harOpprettetOppgaveMedId("123"))
    }
}
