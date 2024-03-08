package mottak

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mottak.arena.ArenaClientImpl
import mottak.behandlingsflyt.BehandlingsflytClientImpl
import mottak.gosys.GosysClientImpl
import mottak.joark.JoarkClientImpl
import mottak.kafka.createTopology
import mottak.pdl.PdlClient
import mottak.pdl.PdlClientImpl
import mottak.saf.SafClientImpl
import no.nav.aap.kafka.streams.v2.KafkaStreams
import no.nav.aap.kafka.streams.v2.Streams
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val SECURE_LOG: Logger = LoggerFactory.getLogger("secureLog")
val APP_LOG = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOG.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server(
    config: Config = Config(),
    kafka: Streams = KafkaStreams(),
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = prometheus
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            LogbackMetrics(),
        )
    }

    environment.monitor.subscribe(ApplicationStopping) {
        kafka.close()
    }

    val joark = JoarkClientImpl(config)
    val kelvin = BehandlingsflytClientImpl(config)
    val pdl = PdlClientImpl(config)
    val arena = ArenaClientImpl(config)
    val gosys = GosysClientImpl(config)
    val saf = SafClientImpl(config)

    val topology = createTopology(saf, joark, pdl, kelvin, arena, gosys)

    kafka.connect(
        topology = topology,
        config = config.kafka,
        registry = prometheus,
    )

    routing {
        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }

            get("/live") {
                call.respond(HttpStatusCode.OK, "live")
            }

            get("/ready") {
                when (kafka.ready()) {
                    true -> call.respond("ready")
                    else -> call.respond(HttpStatusCode.ServiceUnavailable, "Kafka not ready")
                }
            }
        }
    }
}
