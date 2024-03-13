package mottak

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import mottak.enhet.EnhetService
import mottak.kafka.MottakTopology
import mottak.kafka.Topics
import no.nav.aap.kafka.streams.v2.test.StreamsMock
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StreamsTest {

    @Test
    fun `Test hele greia`() {
        val kafka = StreamsMock()
        val config = TestConfig()
        val topics = Topics(config.kafka)
        val topology = MottakTopology(
            config,
            SafFake,
            JoarkFake,
            PdlFake,
            BehandlingsflytFake,
            ArenaFake,
            GosysFake,
            EnhetService(
                NorgFake,
                SkjermingFake,
            ),
        )

        kafka.connect(
            topology = topology(),
            config = config.kafka,
            registry = SimpleMeterRegistry(),
        )

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

        assertTrue(GosysFake.harOpprettetOppgave("123", "oslo"))
        assertTrue(ArenaFake.harOpprettetOppgaveMedId("123"))
    }
}
