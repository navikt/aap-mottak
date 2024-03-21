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
import mottak.oppgave.Oppgave
import mottak.oppgave.OppgaveClient
import mottak.joark.Joark
import mottak.joark.JoarkClient
import mottak.pdl.Pdl
import mottak.pdl.PdlClient
import mottak.saf.Saf
import mottak.saf.SafClient

private val IGNORED_MOTTAKSKANAL = listOf(
    "EESSI",
    "NAV_NO_CHAT",
    "EKST_OPPS"
)

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
            .processor(MeterConsumed(registry))
            .map { record -> saf.hentJournalpost(record.journalpostId) }
            .filter { it.erSkjemaTilAAP() }
            .filter {
                val erJournalført = !it.erJournalført()
                if (erJournalført) {
                    registry.counter("kafka_streams_consumed_journalfort").increment()
                }
                erJournalført
            }
            .map { jp -> enrichWithNavEnhet(jp) }
            .forEach(::håndterJournalpost)
    }

    private fun enrichWithNavEnhet(journalpost: Journalpost): Pair<Journalpost, NavEnhet> {
        return when (journalpost) {
            is Journalpost.MedIdent -> {
                val personopplysninger = pdl.hentPersonopplysninger(journalpost.personident)

                @Suppress("NAME_SHADOWING")
                val journalpost = journalpost.copy(personident = personopplysninger.personident)
                journalpost to enhetService.getNavEnhet(personopplysninger)
            }

            is Journalpost.UtenIdent -> {
                journalpost to enhetService.getNavEnhetForFordelingsoppgave()
            }
        }
    }

    private fun håndterJournalpost(
        @Suppress("UNUSED_PARAMETER")
        key: String,
        journalpostMedEnhet: Pair<Journalpost, NavEnhet>,
    ) {
        val (journalpost, enhet) = journalpostMedEnhet

        when (journalpost) {
            is Journalpost.MedIdent -> håndterJournalpost(journalpost, enhet)
            is Journalpost.UtenIdent -> håndterJournalpost(journalpost)
        }

        joark.oppdaterJournalpost(journalpost, enhet)
    }

    private fun håndterJournalpost(journalpost: Journalpost.UtenIdent) {
        SECURE_LOG.info("Forsøker å rute journalpost uten ident")
        oppgave.opprettOppgaveForManglendeIdent(journalpost)
    }

    private fun håndterJournalpost(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        SECURE_LOG.info("Forsøker å rute journalpost med ident")
        if (journalpost.erPliktkort()) {
            error("not implemented")
        }

        if (arena.finnesSak(journalpost)) {
            oppgave.opprettManuellJournalføringsoppgave(journalpost)
        } else if (skalTilKelvin(journalpost)) {
            kelvin.finnEllerOpprettSak(journalpost)
        } else {
            sakIkkeFunnet(journalpost, enhet)
        }
    }

    private fun skalTilKelvin(journalpost: Journalpost): Boolean {
        return false
    }

    private fun sakIkkeFunnet(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        SECURE_LOG.info("Ingen eksisterende saker funnet for person.")
        when {
            journalpost.erEttersending() -> {
                oppgave.opprettManuellJournalføringsoppgave(journalpost)
            }

            journalpost.erSøknad() -> arenaOppgave(journalpost, enhet)
            journalpost.erPliktkort() -> {}
            else -> error("Uhåndtert skjema for journalpostId ${journalpost.journalpostId}")
        }
    }

    private fun arenaOppgave(journalpost: Journalpost.MedIdent, enhet: NavEnhet) {
        val saksnummer = arena.opprettOppgave(journalpost)
        oppgave.opprettAutomatiskJournalføringsoppgave(journalpost, enhet)
    }
}
