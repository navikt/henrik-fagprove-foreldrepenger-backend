package no.nav.fagprove.foreldrepenger.domain

data class Beregningsgrunnlag(
    val arssats: Int,
    val begrensetTilSeksG: Int,
    val kreverManuellVurdering: Boolean,
    val begrunnelse: String? = null
)