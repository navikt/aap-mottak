package mottak.saf

import mottak.graphql.GraphQLError
import mottak.graphql.GraphQLExtensions
import java.time.LocalDateTime

data class SafRespons(
    val data: SafData?,
    val errors: List<GraphQLError>? = null,
    val extensions: GraphQLExtensions? = null,
)

data class SafData(
    val journalpostById: SafJournalpost?
)

data class SafJournalpost(
    /**
     * Unik identifikator per Journalpost
     * @example: 123456789
     */
    val journalpostId: Long,

    /**
     * Beskriver innholdet i journalposten samlet,
     * @example: Ettersendelse til søknad om AAP
     */
    val tittel: String?  = null,

    /**
     * Inngående, utgående eller notat
     */
    val journalposttype: Journalposttype,

    /**
     * Status på journalposten i joark
     * @example: MOTTATT
     */
    val journalstatus: Journalstatus? = null,

    /**
     *  Temaet/fagområdet som journalposten og tilhørende sak tillhører
     *  For sakstilknyttede journalposter, er det tema på SAK- eller PSAK-saken som er gjeldende tema.
     *  For journalposter som enda ikke har fått sakstilknytning, returneres tema på journalposten.
     *  @example: AAP
     */
    val tema: String? = null,

    /**
     * Sier hvilken sak journalposten er knyttet til.
     * En journalpost kan maksimalt være knyttet til èn sak,
     * men et dokument kan være knyttet til fler journalposter og dermed fler saker.
     */
    val sak: Sak,

    /**
     * Personen eller organisasjonen som er avsender av dokumentene i journalposten.
     */
    val avsender: AvsenderMottaker? = null,

    /**
     * Personen eller organisasjonen som er mottaker av dokumentene i journalposten.
     */
    val mottaker: AvsenderMottaker? = null,

    /**
     * Kanalen dokumentene ble mottatt i eller sendt ut på.
     * Dersom journalposten ikke har noen kjent kanal, returneres [Kanal.UKJENT]
     * @example: ALTINN
     */
    val kanal: Kanal? = null,

    /**
     * Brukes for sporing og feilsøking på tvers av systemer.
     * Referansen er idempotent (unique constraint i joark sin database)
     */
    val eksternReferanseId: String? = null,

    /**
     * Liste over datoer som kan være relevante for denne journalposten.
     * Typen avhenger av journalposttypen.
     * @example: DATO_EKSPEDERT
     */
    val relevanteDatoer: List<SafRelevantDato?>,

    /**
     * Liste over dokumentinfo tilknyttet journalposten.
     */
    val dokumenter: List<SafDokumentInfo?>? = null,
)

enum class Kanal {
    ALTINN,
    EESSI,
    EIA,
    EKST_OPPS,
    LOKAL_UTSKRIFT,
    NAV_NO,
    SENTRAL_UTSKRIFT,
    SDP,
    SKAN_NETS,
    SKAN_PEN,
    SKAN_IM,
    TRYGDERETTEN,
    HELSENETTET,
    INGEN_DISTRIBUSJON,
    NAV_NO_UINNLOGGET,
    INNSENDT_NAV_ANSATT,
    NAV_NO_CHAT,
    DPV,
    E_POST,
    ALTINN_INNBOKS,
    UKJENT,
}

enum class Journalposttype {
    /**
     * Inngående dokument som NAV har mottatt
     */
    I,

    /**
     * Utgående dokument som NAV har produsert
     */
    U,

    /**
     * Notat for internt bruk som NAV har produsert selv
     */
    N,
}

enum class Journalstatus {
    /**
     * Journalposten er mottatt men ikke journalført.
     * "Mottatt" er et annet begrep for "arkivert" eller "midlertidig journalført".
     * Statusen vil kun forekomme for inngående dokumenter.
     */
    MOTTATT,

    /**
     * Journalposten er "ferdigstilt".
     * Videre behandling av forsendelsen er overført til fagsystemet.
     */
    JOURNALFOERT,

    /**
     * Journalposten med tilhørende dokumenter er ferdigstilt,
     * og journalen er i prinsippet låst for videre endringer.
     * Tilsvarer statusen [JOURNALFOERT] for inngående dokumenter
     */
    FERDIGSTILT,

    /**
     * Dokumentet er sendt til bruker og/eller tilgjengelig på DittNAV.
     */
    EKSPEDERT,

    /**
     * Journalposten er opprettet i arkivet, men fremdeles under arbeid.
     * Forekommer for utgående dokumenter og notater.
     */
    UNDER_ARBEID,

    /**
     * Journalposten har blitt unntatt fra saksbehandling etter at
     * den feilaktig har blitt knyttet til en sak.
     * Det er ikke mulig å slette en saksrelasjon.
     */
    FEILREGISTRERT,

    /**
     * Journalposten er unntatt fra saksbehandling.
     * Brukes som regel fed feilsituasjoner knyttet til skanning og journalføring.
     * Status vil kun forekomme for inngående dokumenter.
     */
    UTGAAR,

    /**
     * Utgående dokumenter kan avbrytes mens de er under arbeid.
     * Statusen brukes ved feilsituasjoner rundt vedtaksproduksjon.
     */
    AVBRUTT,

    /**
     * Finner ingen kjent bruker.
     * Brukes når det ikke er muligå journalføre pga manglende identifikasjon
     * Status kan forekomme for inngående dokumenter.
     */
    UKJENT_BRUKER,

    /**
     * Benyttes i forbindelse med brevproduksjon for å reservere "plass" i dokumentet.
     * Tilsvarer [OPPLASTING_DOKUMENT] for inngående dokumenter
     */
    RESERVERT,

    /**
     * Midlertidig status på vei mot [MOTTATT].
     * Dersom en journalpost blir stående i status [OPPLASTING_DOKUMENT] over tid,
     * tyder dette på at noe har gått feil under opplasting av vedlegg ved arkivering.
     * Kan forekomme for inngående dokumenter.
     */
    OPPLASTING_DOKUMENT,

    /**
     * Statusfeltet i Joark er tomt, og oversettes til status [UKJENT]
     */
    UKJENT,
}

data class SafRelevantDato(
    val dato: LocalDateTime,
    val datotype: SafDatoType
)

data class Sak(
    /**
     * Saksnummer i fagsystemet
     */
    val fagsakId: String? = null,

    /**
     * Kode som indikerer hvilket fagsystem, evt nummerserie for fagsaker.
     */
    val fagsaksystem: String? = null,

    /**
     * Sier hvorvidt saken inngår i et fagsystem [Sakstype.FAGSAK]
     * eller ikke [Sakstype.GENERELL_SAK]
     */
    val sakstype: Sakstype,
)

enum class Sakstype {
    /**
     * Vil si at saken tilhører et fagsystem.
     * Hvilket fagsystem spesifiseres i feltet "fagsaksystem"
     */
    FAGSAK,

    /**
     * Benyttes normalt for dokumenter som ikke saksbehandles i et fagsystem.
     */
    GENERELL_SAK
}

data class AvsenderMottaker(
    /**
     * Identifikatoren til parten som er avsender ellermottaker.
     * Normalt et fødselsnummer eller organisasjonsnummer.
     */
    val id: String,

    /**
     * Identifikatoren sin type
     * @example: [AvsenderMottakerIdType.FNR]
     */
    val type: AvsenderMottakerIdType,
)

enum class AvsenderMottakerIdType {
    /**
     * Folkeregistrert fnr eller dnr
     */
    FNR,

    /**
     * Foretaksregisteret orgnummer for en jyuridisk person
     */
    ORGNR,

    /**
     * Helsepersonellregisterets id for leger og annet helsepersonell
     */
    HPRNR,

    /**
     * Unik ID for utenlandske institusjoner/organisasjoner.
     * Identifikatorene vedlikeholdes i EUs institusjonskatalog.
     */
    UTL_ORG,

    /**
     *  AvsenderMottakerId er tom.
     */
    NULL,

    /**
     * Ukjent type
     */
    UKJENT,
}

enum class SafDatoType {
    DATO_OPPRETTET, DATO_SENDT_PRINT, DATO_EKSPEDERT,
    DATO_JOURNALFOERT, DATO_REGISTRERT,
    DATO_AVS_RETUR, DATO_DOKUMENT
}

data class SafDokumentInfo(
    val dokumentInfoId: String,
    val brevkode: String? = null,
    val tittel: String? = null,
    val dokumentvarianter: List<SafDokumentvariant?>
)

data class SafDokumentvariant(
    val variantformat: SafVariantformat,
    val brukerHarTilgang: Boolean,
    val filtype: String
)

enum class SafVariantformat {
    ARKIV, SLADDET, ORIGINAL
}

