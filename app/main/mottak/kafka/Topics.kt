package mottak.kafka

import libs.kafka.AvroSerde
import libs.kafka.StreamsConfig
import libs.kafka.Topic
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class Topics(private val config: StreamsConfig) {
    // todo: ACL https://github.com/navikt/dokumenthandtering-iac/blob/master/kafka-aiven/aapen-dok-journalfoering/topic.yaml
    val journalfoering = Topic(
        name = "teamdokumenthandtering.aapen-dok-journalfoering",
        valueSerde = AvroSerde.specific<JournalfoeringHendelseRecord>(config),
    )
}
