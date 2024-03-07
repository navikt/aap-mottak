package mottak.lokalkontor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mottak.Config
import mottak.lokalkontor.PdlResponse.Data.GeografiskTilknytning.GTException
import java.net.URI
import java.util.*

data class PdlConfig(
    val host: URI,
    val scope: String,
    val audience: String,
)

class PdlClient(config: Config) {
    private val httpClient = HttpClient(CIO) // todo bytt med http client factory
    private val url = config.pdl.host.toURL()

    fun hentGTogGradering(personident: String): PdlResponse.Data {
        val query = PdlRequest.hentGtOgGradering(personident)
        return fetch(query)
    }

    private fun fetch(query: PdlRequest): PdlResponse.Data {
        return runBlocking {
            val response = httpClient.post(url) {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", UUID.randomUUID())
                header("TEMA", "AAP")
                // bearerAuth(azure.getClientCredential())
                contentType(ContentType.Application.Json)
                setBody(query)
            }

            fun PdlResponse.tryIntoData(): PdlResponse.Data {
                return when (errors.isNullOrEmpty()) {
                    true -> data ?: error("missing data in pdl response")
                    false -> error("pdl response has errors: $errors")
                }
            }

            when (response.status.value) {
                in 200..299 -> response.body<PdlResponse>().tryIntoData()
                in 400..499 -> error("Client error calling PDL ($url): ${response.status}")
                in 500..599 -> error("Server error calling PDL ($url): ${response.status}")
                else -> error("Unknown error calling PDL ($url): ${response.status}")
            }
        }
    }
}

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
        }
    }
"""

data class PdlResponse(
    val data: Data?,
    val errors: List<Error>?,
) {
    val gradering: String
        get() = data
            ?.hentPerson
            ?.adressebeskyttelse
            ?.singleOrNull()
            ?.gradering
            ?: "UGRADERT"

    val geografiskTilknytning: String
        get() = data
            ?.hentGeografiskTilknytning
            ?.tryIntoString()
            ?: throw GTException("Mangler GT fra PDL response.")

    data class Data(
        val hentGeografiskTilknytning: GeografiskTilknytning?,
        val hentPerson: Person?,
    ) {
        data class Person(
            val adressebeskyttelse: List<Adressebeskyttelse>,
            val navn: List<Navn>,
        )

        data class Adressebeskyttelse(val gradering: String)

        data class GeografiskTilknytning(
            val gtLand: String?,
            val gtKommune: String?,
            val gtBydel: String?,
            val gtType: Type,
        ) {
            class GTException(msg: String) : RuntimeException(msg)

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

    data class Error(
        val message: String,
        val locations: List<Location>,
        val path: List<String>?,
        val extensions: Extensions
    ) {
        data class Location(val line: Int?, val column: Int?)
        data class Extensions(val code: String?, val classification: String)
    }
}
