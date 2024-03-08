package mottak.kafka

import no.nav.aap.kafka.serde.avro.AvroSerde
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.serde.StreamSerde
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

object Topics {
    val journalfoering = Topic(
        name = "teamdokumenthandtering.aapen-dok-journalfoering",
        valueSerde = joarkAvroSerde(),
    )

    private fun joarkAvroSerde(): StreamSerde<JournalfoeringHendelseRecord> =
        object : StreamSerde<JournalfoeringHendelseRecord> {
            private val internal = AvroSerde.specific<JournalfoeringHendelseRecord>()
            override fun serializer(): Serializer<JournalfoeringHendelseRecord> = internal.serializer()
            override fun deserializer(): Deserializer<JournalfoeringHendelseRecord> = internal.deserializer()
        }
}
