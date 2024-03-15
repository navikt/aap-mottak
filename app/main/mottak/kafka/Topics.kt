package mottak.kafka

import libs.kafka.AvroSerde
import libs.kafka.StreamsConfig
import libs.kafka.Topic
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class Topics(config: StreamsConfig) {
    val journalfoering = Topic(
        name = "teamdokumenthandtering.aapen-dok-journalfoering",
        valueSerde = AvroSerde.specific<JournalfoeringHendelseRecord>(config),
    )
}
