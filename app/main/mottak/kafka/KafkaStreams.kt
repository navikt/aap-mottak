package mottak.kafka

import io.micrometer.core.instrument.MeterRegistry
import libs.kafka.KeyValue
import libs.kafka.Topology
import libs.kafka.processor.Processor
import libs.kafka.processor.ProcessorMetadata
import libs.kafka.topology
import mottak.Config
import mottak.Journalpost
import mottak.SECURE_LOG
import mottak.behandlingsflyt.Behandlingsflyt
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.enhet.EnhetService
import mottak.enhet.NavEnhet
import mottak.enhet.NorgClient
import mottak.enhet.SkjermingClient
import mottak.joark.Joark
import mottak.joark.JoarkClient
import mottak.pdl.PdlClient
import mottak.saf.Saf
import mottak.saf.SafClient

class MeterConsumed<T>(
    private val registry: MeterRegistry,
) : Processor<T, T>("consume-journalfoering-metrics") {
    override fun process(metadata: ProcessorMetadata, keyValue: KeyValue<String, T>): T {
        registry.counter("kafka_streams_consumed_filtered").increment()
        return keyValue.value
    }
}

class MottakTopology(
    config: Config,
    private val registry: MeterRegistry,
    private val saf: Saf = SafClient(config),
    private val joark: Joark = JoarkClient(config),
    private val kelvin: Behandlingsflyt = BehandlingsflytClient(config),
    private val enhetService: EnhetService = EnhetService(
        NorgClient(config),
        SkjermingClient(config),
        PdlClient(config)
    ),
) {
    private val topics = Topics(config.kafka)

    operator fun invoke(): Topology = topology {
        consume(topics.journalfoering)
            .secureLog { info("Record -- ID: ${it.journalpostId}, TEMA: ${it.temaNytt}, KANAL: ${it.mottaksKanal}, BEHANDLINGSTEMA: ${it.behandlingstema}") }
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "MOTTATT" }
            .filter { record -> record.mottaksKanal !in listOf("EESSI") }
            .processor(MeterConsumed(registry))
            .map { record -> saf.hentJournalpost(record.journalpostId) }
            .filter { jp -> jp.harFortsattTilstandMottatt() }
            .map { jp -> enhetService.enrichWithNavEnhet(jp) }
            .forEach(::håndterJournalpost)
    }

    private fun håndterJournalpost(
        journalpostMedEnhet: Pair<Journalpost, NavEnhet>,
    ) {
        val (journalpost, enhet) = journalpostMedEnhet

        if (!journalpost.erSøknad()) {
            SECURE_LOG.info("For tiden hopper vi over alt som ikke er søknad (${journalpost.journalpostId}).")
            return
        }

        when (journalpost) {
            is Journalpost.MedIdent -> håndterJournalpostMedIdent(journalpost, enhet)
            is Journalpost.UtenIdent -> håndterJournalpostUtenIdent(journalpost)
        }
    }

    private fun håndterJournalpostMedIdent(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        SECURE_LOG.info("Forsøker å rute journalpost ${journalpost.journalpostId} med ident")

        val saksinfo = kelvin.finnEllerOpprettSak(journalpost)
        SECURE_LOG.info("Opprettet sak i Kelvin med saksnummer ${saksinfo.saksnummer}")
        joark.oppdaterJournalpost(journalpost, enhet, saksinfo.saksnummer, journalpost.personident.ident)
        joark.ferdigstillJournalpost(journalpost, enhet)
        saf.hentJson(journalpost.journalpostId)?.let {
            kelvin.sendSøknad(saksinfo.saksnummer, journalpost.journalpostId, it)
        } ?: SECURE_LOG.warn("Journalpost ${journalpost.journalpostId} hadde ikke json")
    }

    private fun håndterJournalpostUtenIdent(journalpost: Journalpost.UtenIdent) {
        SECURE_LOG.info("Forsøker å rute journalpost ${journalpost.journalpostId} uten ident")
    }
}
