package no.nav.fagprove.foreldrepenger.domain

data class Inntektsregistrering(
    val maned: String,
    val type: Inntektstype,
    val belop: Int
)

enum class Inntektstype {
    ARBEID,
    SYKEPENGER,
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    DAGPENGER,
    AAP,
    PLEIEPENGER,
    STIPEND_LANEKASSEN
}