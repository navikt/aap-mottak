package mottak.enhet

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.Ident
import mottak.graphql.GraphQLError
import mottak.http.HttpClientFactory
import mottak.http.tryInto
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import java.net.URI
import java.util.*

enum class PdlGradering {
    STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND, FORTROLIG, UGRADERT
}

data class PdlConfig(
    val host: URI,
    val scope: String,
    val audience: String,
)

class GTException(msg: String) : RuntimeException(msg)

class PdlClient(private val config: Config) {
    private val httpClient = HttpClientFactory.create()
    private val url = config.pdl.host.toURL()
    private val tokenProvider = AzureAdTokenProvider(config.azure, httpClient)

    fun hentPersonopplysninger(ident: Ident): Personopplysninger {
        val personident = when (ident) {
            is Ident.Personident -> ident.id
            is Ident.Aktørid -> ident.id
        }

        val query = PdlRequest.hentGtOgGradering(personident)
        val data = fetch(query)

        return Personopplysninger(
            personident = data.personident,
            gradering = PdlGradering.valueOf(data.gradering),
            gt = data.geografiskTilknytning
        )
    }

    private fun fetch(query: PdlRequest): PdlResponse.Data {
        return runBlocking {
            val token = tokenProvider.getClientCredentialToken(config.gosys.scope)
            httpClient.post(url) {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", UUID.randomUUID())
                header("TEMA", "AAP")
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(query)
            }
                .tryInto<PdlResponse>()
                .takeDataOrThrow()
        }
    }
}

data class Personopplysninger(
    val personident: String,
    val gradering: PdlGradering,
    val gt: String
)

data class PdlRequest(val query: String, val variables: Variables) {
    data class Variables(val ident: String)

    companion object {
        fun hentGtOgGradering(personident: String) = PdlRequest(
            query = gtOgGradering.replace("\n", ""),
            variables = Variables(personident),
        )
    }
}

// escape $ in multiline strings
private const val ident = "\$ident"

private const val gtOgGradering = """
    query($ident: ID!) {
        hentGeografiskTilknytning(ident: $ident) {
            gtKommune
            gtBydel
            gtLand
        }
        hentPerson(ident: $ident) {
            adressebeskyttelse {
                gradering
            }
            folkeregisteridentifikator {
                identifikasjonsnummer
            }
        }
    }
"""

fun PdlResponse.takeDataOrThrow(): PdlResponse.Data {
    return when (errors.isNullOrEmpty()) {
        true -> data ?: error("missing data in pdl response")
        false -> error("pdl response has errors: $errors")
    }
}

data class PdlResponse(
    val data: Data?,
    val errors: List<GraphQLError>?,
) {
    data class Data(
        val hentGeografiskTilknytning: GeografiskTilknytning?,
        val hentPerson: Person?,
    ) {
        val personident: String
            get() = hentPerson
                ?.folkeregisteridentifikator
                ?.singleOrNull()
                ?.identifikasjonsnummer
                ?: throw GTException("Mangler personidentifikator fra PDL response.")

        val gradering: String
            get() = hentPerson
                ?.adressebeskyttelse
                ?.singleOrNull()
                ?.gradering
                ?: "UGRADERT"

        val geografiskTilknytning: String
            get() = hentGeografiskTilknytning
                ?.tryIntoString()
                ?: throw GTException("Mangler GT fra PDL response.")

        data class Person(
            val folkeregisteridentifikator: List<FolkeregisterIdentifikator>,
            val adressebeskyttelse: List<Adressebeskyttelse>,
            val navn: List<Navn>,
        )

        data class FolkeregisterIdentifikator(
            val identifikasjonsnummer: String,
        )

        data class Adressebeskyttelse(val gradering: String)

        data class GeografiskTilknytning(
            val gtLand: String?,
            val gtKommune: String?,
            val gtBydel: String?,
            val gtType: Type,
        ) {
            enum class Type {
                KOMMUNE,
                BYDEL,
                UTLAND,
                UDEFINERT
            }

            /**
             * TODO: spesielle GT
             * Oslo
             *     Bydel Marka i Oslo har ikke eget NAV-kontor.
             *     De som er bosatt der får GT i en av nabobydelene, valgt ut i fra deres adresse.
             *
             *     Innbyggere med bydel Sentrum (Oslo) vil i PDL ha bydelsnummer 030116.
             *     Saksbehandlingen som gjelder de personene skal alle håndteres av NAV-kontoret på St.Hanshaugen.
             *
             * Svalbard
             *      I motsetning til TPS vil man i PDL få opp Svalbard som kommune i GT.
             *      Saksbehandling som gjelder personer med GT på Svalbard, håndteres av NAV-kontoret i Tromsø.
             */
            fun tryIntoString(): String {
                return when (gtType) {
                    Type.KOMMUNE -> tryIntoKommune()
                    Type.BYDEL -> tryIntoBydel()
                    Type.UTLAND -> tryIntoUtland()
                    Type.UDEFINERT -> Type.UDEFINERT.name
                }
            }

            private fun tryIntoKommune(): String {
                if (gtKommune == null || gtKommune.length != 4) {
                    throw GTException(
                        """
                        Felt:       GT   
                        Type:       $gtType
                        Verdi:      $gtKommune
                        Validering: Kommune i GT fra PDL var ikke 4 siffer, men ${gtKommune?.length}
                        Merknad:    Kan være avvik mellom registrert adresse og kartverkets nasjonale adresseregister.
                                    Døde personer har GT fra siste bodstedsadresse, ved endring av kommunenummer etter
                                    dødsfall, så vil ikke personens GT bli oppdatert til nytt kommunenummer.
                                    Avvik som oppdager på adresser må rettes hos 'master' (datakilde) for adressen.
                        """.trimIndent()
                    )
                }

                return gtKommune
            }

            private fun tryIntoBydel(): String {
                if (gtBydel == null || gtBydel.length != 6) {
                    throw GTException(
                        """
                        Felt:       GT   
                        Type:       $gtType
                        Verdi:      $gtBydel
                        Validering: Bydel i GT fra PDL var ikke 6 siffer, men ${gtBydel?.length}
                        Merknad:    Kan være avvik mellom registrert adresse og kartverkets nasjonale adresseregister.
                                    I noen tilfeller har vi bare kommunenummer på kommuner som skal ha bydel
                                    (Oslo, Bergen, Stavenger og Trondheim).
                                    Årsaken er ufullstendig adresse eller feil i adresse.
                                    Avvik som oppdager på adresser må rettes hos 'master' (datakilde) for adressen.
                        """.trimIndent()
                    )
                }
                return gtBydel
            }

            private fun tryIntoUtland(): String {
                if (gtLand == null || gtLand.length != 3) {
                    throw GTException(
                        """
                        Felt:       GT   
                        Type:       $gtType
                        Verdi:      $gtLand
                        Validering: Land i GT fra PDL var ikke 3 siffer, men ${gtLand?.length}
                        Merknad:    Kun utfylt dersom gtType=UTLAND og personen har en gyldig utenlandsk adresse
                        """.trimIndent()
                    )
                }
                return gtLand
            }
        }

        data class Navn(
            val fornavn: String,
            val etternavn: String,
            val mellomnavn: String?,
        )
    }
}
