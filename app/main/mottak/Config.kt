package mottak

import libs.kafka.StreamsConfig
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import java.net.URI

fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

open class Config(
    val pdl: PdlConfig = PdlConfig(),
    val skjerming: SkjermingConfig = SkjermingConfig(),
    val norg: NorgConfig = NorgConfig(),
    val kafka: StreamsConfig = StreamsConfig(),
    val fssProxy: FssProxyConfig = FssProxyConfig(),
    val gosys: GosysConfig = GosysConfig(),
    val saf: SafConfig = SafConfig(),
    val joark: JoarkConfig = JoarkConfig(),
    val azure: AzureConfig = AzureConfig(),
)

data class FssProxyConfig(
    val host: String = getEnvVar("FSS_PROXY_HOST"),
    val scope: String = getEnvVar("FSS_PROXY_SCOPE"),
)

data class GosysConfig(
    val host: String = getEnvVar("GOSYS_OPPGAVE_HOST"),
    val scope: String = getEnvVar("GOSYS_OPPGAVE_SCOPE"),
)

data class SafConfig(
    val host: String = getEnvVar("SAF_HOST"),
    val scope: String = getEnvVar("SAF_SCOPE"),
)

data class JoarkConfig(
    val host: String = getEnvVar("JOARK_HOST"),
    val scope: String = getEnvVar("JOARK_SCOPE"),
)

data class NorgConfig(
    val host: String = getEnvVar("NORG_HOST"),
)

data class SkjermingConfig(
    val host: String = getEnvVar("SKJERMING_HOST"),
)

data class PdlConfig(
    val host: URI = getEnvVar("PDL_HOST").let(::URI),
    val scope: String = getEnvVar("PDL_SCOPE"),
    val audience: String = getEnvVar("PDL_AUDIENCE"),
)
