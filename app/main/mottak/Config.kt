package mottak

import libs.kafka.SchemaRegistryConfig
import libs.kafka.StreamsConfig
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import java.net.URI

open class Config(
    val pdl: PdlConfig = PdlConfig(),
    val skjerming: SkjermingConfig = SkjermingConfig(),
    val norg: NorgConfig = NorgConfig(),
    val kafka: StreamsConfig = StreamsConfig(schemaRegistry = SchemaRegistryConfig()),
    val fssProxy: FssProxyConfig = FssProxyConfig(),
    val gosys: OppgaveConfig = OppgaveConfig(),
    val saf: SafConfig = SafConfig(),
    val joark: DokarkivConfig = DokarkivConfig(),
    val azure: AzureConfig = AzureConfig(),
)

data class FssProxyConfig(
    private val env: Env = getEnv(),
    val host: URI = "https://aap-fss-proxy.$env-fss-pub.nais.io".let(::URI),
    val scope: String = "api://$env-fss.aap.fss-proxy/.default",
)

data class OppgaveConfig(
    private val env: Env = getEnv(),
    val host: URI = "https://oppgave.$env-fss-pub.nais.io".let(::URI),
    val scope: String = "api://$env-fss.oppgavehandtering.oppgave/.default",
)

data class SafConfig(
    private val env: Env = getEnv(),
    private val fssEnv: String = System.getenv("FSS_ENV") ?: "",
    val host: URI = "https://saf$fssEnv.$env-fss-pub.nais.io".let(::URI),
    val scope: String = "api://$env-fss.teamdokumenthandtering.saf/.default",
)

data class DokarkivConfig(
    private val env: Env = getEnv(),
    private val fssEnv: String = System.getenv("FSS_ENV") ?: "",
    val host: URI = "https://dokarkiv$fssEnv.$env-fss-pub.nais.io".let(::URI),
    val scope: String = "api://$env-fss.teamdokumenthandtering.dokarkiv$fssEnv/.default",
)

data class SkjermingConfig(
    private val env: Env = getEnv(),
    val host: URI = "http://skjermede-personer-pip.nom".let(::URI),
    val scope: String = "api://$env-gcp.nom.skjermede-personer-pip/.default",
)

data class NorgConfig(
    private val env: Env = getEnv(),
    val host: URI = "https://norg2.$env-fss-pub.nais.io".let(::URI),
)

class PdlConfig(
    private val env: Env = getEnv(),
    val host: URI = "https://pdl-api.$env-fss-pub.nais.io/graphql".let(::URI),
    val scope: String = "api://$env-fss.pdl.pdl-api/.default",
)

fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

fun getEnv(): Env = getEnvVar("NAIS_CLUSTER_NAME")
    .substringBefore("-")
    .let(Env::from)

enum class Env {
    dev,
    prod;

    companion object {
        fun from(env: String) = valueOf(env)
    }
}
