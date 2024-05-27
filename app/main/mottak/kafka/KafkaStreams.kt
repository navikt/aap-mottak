package mottak.kafka

import io.micrometer.core.instrument.MeterRegistry
import libs.kafka.KeyValue
import libs.kafka.Topology
import libs.kafka.processor.Processor
import libs.kafka.processor.ProcessorMetadata
import libs.kafka.topology
import mottak.Config
import mottak.Journalpost
import mottak.behandlingsflyt.Behandlingsflyt
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.joark.Joark
import mottak.joark.JoarkClient
import mottak.saf.Saf
import mottak.saf.SafClient
import org.slf4j.LoggerFactory

class MeterConsumed<T>(
    private val registry: MeterRegistry,
) : Processor<T, T>("consume-journalfoering-metrics") {
    override fun process(metadata: ProcessorMetadata, keyValue: KeyValue<String, T>): T {
        registry.counter("kafka_streams_consumed_filtered").increment()
        return keyValue.value
    }
}

private val log = LoggerFactory.getLogger(MottakTopology::class.java)


class MottakTopology(
    config: Config,
    private val registry: MeterRegistry,
    private val saf: Saf = SafClient(config),
    private val joark: Joark = JoarkClient(config),
    private val kelvin: Behandlingsflyt = BehandlingsflytClient(config),
) {
    private val topics = Topics(config.kafka)

    operator fun invoke(): Topology = topology {
        consume(topics.journalfoering)
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "MOTTATT" }
            .filter { record -> record.mottaksKanal !in listOf("EESSI") } // TODO: Det bør også snakkes om MELDEKORT og andre som ikke skal håndteres fordi de alt hånteres av andre
            .processor(MeterConsumed(registry))
            .map { record -> saf.hentJournalpost(record.journalpostId) }
            .filter { jp -> jp.harFortsattTilstandMottatt() }
            .forEach(::håndterJournalpost)
    }

    private fun håndterJournalpost(
        journalpost: Journalpost,
    ) {
        log.info("Mottatt ${journalpost.journalpostId}, forsøker håndtere.")
        if (!journalpost.erSøknad()) {
            log.info("For tiden hopper vi over alt som ikke er søknad (${journalpost.journalpostId}).")
            return
        }

        when (journalpost) {
            is Journalpost.MedIdent -> håndterJournalpostMedIdent(journalpost)
            is Journalpost.UtenIdent -> håndterJournalpostUtenIdent(journalpost)
        }
    }

    private fun håndterJournalpostMedIdent(journalpost: Journalpost.MedIdent) {
        val saksinfo = kelvin.finnEllerOpprettSak(journalpost)
        joark.oppdaterJournalpost(journalpost, saksinfo.saksnummer)
        joark.ferdigstillJournalpost(journalpost)
        saf.hentJson(journalpost.journalpostId)?.let {
            // TODO: Hvis prosessen tryner her, så vil ikke melding bli kjørt på nytt fordi den har fått
            //       status JOURNALFØRT. Finn en lur fiks her...
            kelvin.sendSøknad(saksinfo.saksnummer, journalpost.journalpostId, it)
        } ?: loggFeilhåndtering(journalpost)
    }

    private fun loggFeilhåndtering(journalpost: Journalpost.MedIdent) {
        log.warn("Journalpost ${journalpost.journalpostId} hadde ikke json, men vi gjør ikke noe med det")
    }

    private fun håndterJournalpostUtenIdent(journalpost: Journalpost.UtenIdent) {
        log.info("Gjør forøvrig ikke noe med journalpost (${journalpost.journalpostId}) uten ident")
    }
}
