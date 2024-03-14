package mottak.kafka

import no.nav.aap.kafka.serde.avro.AvroSerde
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class Topics(private val config: StreamsConfig) {
    // todo: ACL https://github.com/navikt/dokumenthandtering-iac/blob/master/kafka-aiven/aapen-dok-journalfoering/topic.yaml
    val journalfoering = Topic(
        name = "teamdokumenthandtering.aapen-dok-journalfoering",
        valueSerde = AvroSerde.specific<JournalfoeringHendelseRecord>(config),
    )
}
