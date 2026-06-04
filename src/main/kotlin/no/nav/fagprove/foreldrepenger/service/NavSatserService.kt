package no.nav.fagprove.foreldrepenger.service

import no.nav.fagprove.foreldrepenger.config.ForeldrepengerReglerProperties
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class GrunnbeloepResponse(
    val dato: String? = null,
    val grunnbeloep: Int,
    val grunnbeloepPerMaaned: Int? = null,
    val gjennomsnittPerAar: Int? = null,
    val omregningsfaktor: Double? = null,
    val virkningstidspunktForMinsteinntekt: String? = null,
)

data class EngangsstonadResponse(
    val fom: String? = null,
    val tom: String? = null,
    val verdi: Int,
)

data class NavSatser(
    val grunnbeloep: Int,
    val halvG: Int,
    val seksG: Int,
    val engangsstonad: Int,
    val kilde: String,
)

@Service
class NavSatserService(
    private val regler: ForeldrepengerReglerProperties,
) {
    private val webClient = WebClient.builder()
        .baseUrl("https://g.nav.no")
        .build()

    fun hentSatser(): NavSatser {
        return try {
            hentSatserFraNavApi()
        } catch (e: Exception) {
            hentFallbackSatser()
        }
    }

    private fun hentSatserFraNavApi(): NavSatser {
        val grunnbeloep = webClient.get()
            .uri("/api/v1/grunnbeloep")
            .retrieve()
            .bodyToMono(GrunnbeloepResponse::class.java)
            .block()
            ?: throw RuntimeException("Klarte ikke å hente grunnbeløp fra NAV API")

        val engangsstonad = webClient.get()
            .uri("/api/v1/engangsstoenad")
            .retrieve()
            .bodyToMono<EngangsstonadResponse>()
            .block()
            ?: throw RuntimeException("Klarte ikke å hente engangsstønad fra NAV API")

        return NavSatser(
            grunnbeloep = grunnbeloep.grunnbeloep,
            halvG = grunnbeloep.grunnbeloep / 2,
            seksG = grunnbeloep.grunnbeloep * 6,
            engangsstonad = engangsstonad.verdi,
            kilde = "NAV_API",
        )
    }

    private fun hentFallbackSatser(): NavSatser {
        return NavSatser(
            grunnbeloep = regler.grunnbelopFallback,
            halvG = regler.grunnbelopFallback / 2,
            seksG = regler.grunnbelopFallback * 6,
            engangsstonad = regler.engangsstonadFallback,
            kilde = "APPLICATION_YML_FALLBACK",
        )
    }
}