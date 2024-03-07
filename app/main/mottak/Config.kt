package mottak

import mottak.lokalkontor.PdlConfig
import mottak.lokalkontor.SkjermingConfig
import no.nav.aap.kafka.schemaregistry.SchemaRegistryConfig
import no.nav.aap.kafka.streams.v2.config.SslConfig
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import java.net.URI

private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

data class Config(
    val pdl: PdlConfig = PdlConfig(
        host = getEnvVar("PDL_HOST").let(::URI),
        scope = getEnvVar("PDL_SCOPE"),
        audience = getEnvVar("PDL_AUDIENCE"),
    ),
    val skjerming: SkjermingConfig = SkjermingConfig(
        host = getEnvVar("SKJERMING_HOST"),

    ),
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
    val fssProxy: FssProxyConfig = FssProxyConfig(
        baseUrl = getEnvVar("FSS_PROXY_URL"),
        scope = getEnvVar("FSS_PROXY_SCOPE")
    ),
    val gosys: GosysConfig = GosysConfig(
        baseUrl = getEnvVar("GOSYS_OPPGAVE_URL"),
        scope = getEnvVar("GOSYS_OPPGAVE_SCOPE")
    ),
    val saf: SafConfig = SafConfig(
        baseUrl = getEnvVar("SAF_URL"),
        scope = getEnvVar("SAF_SCOPE")
    ),
    val joark: JoarkConfig = JoarkConfig(
        baseUrl = getEnvVar("JOARK_URL"),
        scope = getEnvVar("JOARK_SCOPE")
    ),
    val azure: AzureConfig = AzureConfig(),
)

data class FssProxyConfig(
    val baseUrl: String,
    val scope: String
)

data class GosysConfig(
    val baseUrl: String,
    val scope: String
)

data class SafConfig(
    val baseUrl: String,
    val scope: String
)

data class JoarkConfig(
    val baseUrl: String,
    val scope: String
)

