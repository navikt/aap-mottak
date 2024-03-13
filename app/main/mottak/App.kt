package mottak

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mottak.arena.ArenaClient
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.enhet.EnhetService
import mottak.enhet.NorgClient
import mottak.enhet.SkjermingClient
import mottak.gosys.GosysClient
import mottak.joark.JoarkClient
import mottak.kafka.MottakTopology
import mottak.pdl.PdlClient
import mottak.saf.SafClient
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
        meterBinders += LogbackMetrics()
    }

    environment.monitor.subscribe(ApplicationStopping) {
        kafka.close()
    }

    val topology = MottakTopology(
        joark = JoarkClient(config),
        kelvin = BehandlingsflytClient(config),
        pdl = PdlClient(config),
        arena = ArenaClient(config),
        gosys = GosysClient(config),
        saf = SafClient(config),
        enhetService = EnhetService(
            norg = NorgClient(config),
            skjerming = SkjermingClient(config),
        ),
    )

    kafka.connect(
        topology = topology(),
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
