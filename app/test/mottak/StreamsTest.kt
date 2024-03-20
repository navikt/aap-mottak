package mottak

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import lib.kafka.StreamsMock
import mottak.enhet.EnhetService
import mottak.enhet.NavEnhet
import mottak.kafka.MottakTopology
import mottak.kafka.Topics
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StreamsTest {

    @Test
    fun `Test hele greia`() {
        val kafka = StreamsMock()
        val config = TestConfig()
        val topics = Topics(config.kafka)
        val registry = SimpleMeterRegistry()
        val topology = MottakTopology(
            config,
            registry,
            SafFake,
            JoarkFake,
            PdlFake,
            BehandlingsflytFake,
            ArenaFake,
            OppgaveFake,
            EnhetService(
                NorgFake,
                SkjermingFake,
            ),
        )

        kafka.connect(
            topology = topology(),
            config = config.kafka,
            registry = registry,
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

        assertTrue(ArenaFake.harOpprettetOppgaveMedId("123"))
        assertTrue(OppgaveFake.harOpprettetAutomatiskOppgave("123", NavEnhet("oslo")))
        assertTrue(JoarkFake.harOppdatert("123", NavEnhet("oslo")))
    }
}
