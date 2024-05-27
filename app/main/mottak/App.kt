package mottak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import libs.kafka.KafkaStreams
import libs.kafka.Streams
import mottak.kafka.MottakTopology
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val SECURE_LOG: Logger = LoggerFactory.getLogger("secureLog")
private val log = LoggerFactory.getLogger("no.nav.mottak.App")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> loggUventetFeil(e) }
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

private fun loggUventetFeil(e: Throwable?) {
    log.error("Uhåndter feil", e)
    SECURE_LOG.error("Uhåndtert feil", e)
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

    val topology = MottakTopology(config, prometheus)

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
                when (kafka.live()) {
                    true -> call.respond(HttpStatusCode.OK, "live")
                    else -> call.respond(HttpStatusCode.ServiceUnavailable, "Kafka not live")
                }
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
