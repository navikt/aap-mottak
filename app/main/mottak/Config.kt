package mottak

import no.nav.aap.kafka.schemaregistry.SchemaRegistryConfig
import no.nav.aap.kafka.streams.v2.config.SslConfig
import no.nav.aap.kafka.streams.v2.config.StreamsConfig

private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

data class Config(
    val kafka: StreamsConfig = StreamsConfig(
        brokers = getEnvVar("KAFKA_BROKERS"),
        applicationId = getEnvVar("KAFKA_STREAMS_APPLICATION_ID"),
        ssl = SslConfig(
            truststorePath = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
            keystorePath = getEnvVar("KAFKA_KEYSTORE_PATH"),
            credstorePsw = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        ),
        schemaRegistry = SchemaRegistryConfig(
            url = getEnvVar("KAFKA_SCHEMA_REGISTRY_URL"),
            user = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
            password = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
        ).properties()
    ),
)
