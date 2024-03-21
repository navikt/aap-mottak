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
import mottak.arena.Arena
import mottak.arena.ArenaClient
import mottak.behandlingsflyt.Behandlingsflyt
import mottak.behandlingsflyt.BehandlingsflytClient
import mottak.enhet.EnhetService
import mottak.enhet.NavEnhet
import mottak.enhet.NorgClient
import mottak.enhet.SkjermingClient
import mottak.joark.Joark
import mottak.joark.JoarkClient
import mottak.oppgave.Oppgave
import mottak.oppgave.OppgaveClient
import mottak.pdl.Pdl
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
    private val pdl: Pdl = PdlClient(config),
    private val kelvin: Behandlingsflyt = BehandlingsflytClient(config),
    private val arena: Arena = ArenaClient(config),
    private val oppgave: Oppgave = OppgaveClient(config),
    private val enhetService: EnhetService = EnhetService(NorgClient(config), SkjermingClient(config)),
) {
    private val topics = Topics(config.kafka)

    operator fun invoke(): Topology = topology {
        consume(topics.journalfoering)
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "MOTTATT" }
            .processor(MeterConsumed(registry))
            .map { record -> saf.hentJournalpost(record.journalpostId) }
            .filter { it.erSøknad() }
            .filter { !it.erJournalført() }
            .map { jp -> enrichWithNavEnhet(jp) }
            .forEach(::håndterJournalpost)
    }

    @Suppress("NAME_SHADOWING")
    private fun enrichWithNavEnhet(journalpost: Journalpost): Pair<Journalpost, NavEnhet> {
        return when (journalpost) {
            is Journalpost.MedIdent -> {
                val personopplysninger = pdl.hentPersonopplysninger(journalpost.personident)
                val journalpost = journalpost.copy(personident = personopplysninger.personident)
                val enhet = journalpost.journalførendeEnhet ?: enhetService.getNavEnhet(personopplysninger)
                journalpost to enhet
            }

            is Journalpost.UtenIdent -> {
                journalpost to enhetService.getNavEnhetForFordelingsoppgave()
            }
        }
    }

    private fun håndterJournalpost(
        journalpostMedEnhet: Pair<Journalpost, NavEnhet>,
    ) {
        val (journalpost, enhet) = journalpostMedEnhet

        when (journalpost) {
            is Journalpost.MedIdent -> håndterJournalpost(journalpost, enhet)
            is Journalpost.UtenIdent -> håndterJournalpost(journalpost)
        }
    }

    private fun håndterJournalpost(journalpost: Journalpost.UtenIdent) {
        SECURE_LOG.info("Forsøker å rute journalpost uten ident")
//        joark.oppdaterJournalpost(journalpost, enhet)
//        oppgave.opprettOppgaveForManglendeIdent(journalpost)
    }

    private fun håndterJournalpost(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        SECURE_LOG.info("Forsøker å rute journalpost med ident")
        if (journalpost.erPliktkort()) {
            error("not implemented")
        }

        if (arena.finnesSak(journalpost)) {
            SECURE_LOG.info("Fant eksisterende sak for person i arena.")
//            oppgave.opprettManuellJournalføringsoppgave(journalpost)
        } else if (skalTilKelvin(journalpost)) {
            kelvin.finnEllerOpprettSak(journalpost)
            joark.oppdaterJournalpost(journalpost, enhet)
        } else {
            sakIkkeFunnet(journalpost, enhet)
        }
    }

    private fun skalTilKelvin(journalpost: Journalpost): Boolean {
        return journalpost.erSøknad() // todo: midlertidig måte å si at den skal til kelvin på
    }

    private fun sakIkkeFunnet(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        SECURE_LOG.info("Ingen eksisterende saker funnet for person.")
        when {
            journalpost.erEttersending() -> {
//                oppgave.opprettManuellJournalføringsoppgave(journalpost)
            }

            journalpost.erSøknad() -> arenaOppgave(journalpost, enhet)
            journalpost.erPliktkort() -> {}
            else -> error("Uhåndtert skjema for journalpostId ${journalpost.journalpostId}")
        }
    }

    private fun arenaOppgave(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        SECURE_LOG.info("Oppretter oppgave i Arena for journalpost : ${journalpost.journalpostId}")
//        val saksnummer = arena.opprettOppgave(journalpost)
//        oppgave.opprettAutomatiskJournalføringsoppgave(journalpost, enhet)
    }
}
