package mottak.pdl

import mottak.graphql.GraphQLError

data class PdlResponse(
    val data: Data?,
    val errors: List<GraphQLError>?,
) {
    data class Data(
        val hentGeografiskTilknytning: GeografiskTilknytning?,
        val hentPerson: Person?,
    ) {
        val folkeregisteridentifikator: String
            get() = hentPerson
                ?.folkeregisteridentifikator
                ?.singleOrNull()
                ?.identifikasjonsnummer
                ?: throw MissingPdlOpplysningException("Mangler personidentifikator fra PDL response.")

        val gradering: String
            get() = hentPerson
                ?.adressebeskyttelse
                ?.singleOrNull()
                ?.gradering
                ?: "UGRADERT"

        val geografiskTilknytning: String
            get() = hentGeografiskTilknytning
                ?.tryIntoString()
                ?: throw MissingPdlOpplysningException("Mangler GT fra PDL response.")

        data class Person(
            val folkeregisteridentifikator: List<FolkeregisterIdentifikator>,
            val adressebeskyttelse: List<Adressebeskyttelse>,
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
                    throw MissingPdlOpplysningException(
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
                    throw MissingPdlOpplysningException(
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
                    throw MissingPdlOpplysningException(
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
