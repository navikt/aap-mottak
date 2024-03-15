package mottak

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import libs.kafka.SchemaRegistryConfig
import libs.kafka.SslConfig
import libs.kafka.StreamsConfig
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import java.net.ServerSocket
import java.net.URI

class TestConfig : Config(
    pdl = PdlConfig(
        host = URI.create("http://localhost:${Ports.randomAvailable()}"),
        scope = "",
        env = Env.dev
    ),
    skjerming = SkjermingConfig(
        host = "http://localhost:${Ports.randomAvailable()}".let(::URI),
        env = Env.dev
    ),
    norg = NorgConfig(
        host = "http://localhost:${Ports.randomAvailable()}".let(::URI),
        env = Env.dev
    ),
    kafka = StreamsConfig(
        applicationId = "",
        brokers = "",
        ssl = SslConfig(
            truststorePath = "",
            keystorePath = "",
            credstorePsw = "",
        ),
        schemaRegistry = SchemaRegistryConfig(
            url = "mock://kafka",
            user = "",
            password = "",
        )
    ),
    fssProxy = FssProxyConfig(
        host = "http://localhost:${Ports.randomAvailable()}".let(::URI),
        scope = "",
        env = Env.dev
    ),
    gosys = OppgaveConfig(
        host = "http://localhost:${Ports.randomAvailable()}".let(::URI),
        scope = "",
        env = Env.dev
    ),
    saf = SafConfig(
        host = "http://localhost:${Ports.randomAvailable()}".let(::URI),
        scope = "",
        env = Env.dev
    ),
    joark = DokarkivConfig(
        host = "http://localhost:${Ports.randomAvailable()}".let(::URI),
        scope = "",
        env = Env.dev
    ),
    azure = AzureConfig(
        tokenEndpoint = "http://localhost:${Ports.randomAvailable()}",
        clientId = "",
        clientSecret = "",
        jwksUri = "",
        issuer = "",
    ),
)

private object Ports {
    private val mutex = Mutex()
    private val reserved = mutableSetOf<Int>()

    /**
     * Reserves and returns a random available port on host-machine.
     */
    fun randomAvailable(): Int =
        ServerSocket(0)
            .use(ServerSocket::getLocalPort)
            .also {
                runBlocking {
                    reserve(it)
                }
            }

    private suspend fun reserve(port: Int) {
        mutex.withLock {
            if (port in reserved) return reserve(randomAvailable())
            else reserved.add(port)
        }
    }
}