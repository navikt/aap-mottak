package mottak

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import lib.kafka.StreamsMock
import mottak.enhet.EnhetService
import mottak.enhet.NavEnhet
import mottak.kafka.MottakTopology
import mottak.kafka.Topics
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension

@ExtendWith(SystemStubsExtension::class)
class StreamsTest {

    @SystemStub
    val naisCluster = EnvironmentVariables("NAIS_CLUSTER_NAME", "dev")

    @Test
    fun `Happy path oppretter sak og journalfører`() {
        val config = TestConfig()
        val kafka = setUpStreamsMock(config)
        val topics = Topics(config.kafka)

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

        assertTrue(BehandlingsflytFake.harOpprettetSak(123))
        assertTrue(JoarkFake.harOppdatert(123, NavEnhet(NorgFake.ENHET_NR)))
        assertTrue(JoarkFake.harFerdigstilt(123))
    }

    @Test
    fun `Filtrerer ut alt annet enn AAP`() {
        val config = TestConfig()
        val kafka = setUpStreamsMock(config)
        val topics = Topics(config.kafka)

        val journalføringstopic = kafka.testTopic(topics.journalfoering)
        journalføringstopic.produce("1") {
            JournalfoeringHendelseRecord.newBuilder().apply {
                hendelsesId = "1"
                versjon = 1
                hendelsesType = ""
                journalpostId = 123L
                temaGammelt = "AAP"
                temaNytt = "DAG"
                journalpostStatus = "MOTTATT"
                mottaksKanal = "NAV_NO"
                kanalReferanseId = ""
                behandlingstema = ""
            }.build()
        }

        assertFalse(BehandlingsflytFake.harOpprettetSak(123))
    }

    private fun setUpStreamsMock(config: TestConfig): StreamsMock {
        val kafka = StreamsMock()
        val registry = SimpleMeterRegistry()
        val topology = MottakTopology(
            config,
            registry,
            SafFake,
            JoarkFake,
            BehandlingsflytFake,
            EnhetService(
                NorgFake,
                SkjermingFake,
                PdlFake
            ),
        )

        kafka.connect(
            topology = topology(),
            config = config.kafka,
            registry = registry,
        )

        return kafka
    }
}
