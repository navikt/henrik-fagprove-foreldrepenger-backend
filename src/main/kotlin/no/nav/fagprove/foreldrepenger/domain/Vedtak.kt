package no.nav.fagprove.foreldrepenger.domain

data class Vedtak(
    val soknadId: String,
    val status: VedtakStatus,
    val begrunnelse: String,
    val beregningsgrunnlag: Beregningsgrunnlag? = null,
    val totalStonadsperiodeUker: Int? = null,
    val kvoter: Kvoter? = null,
    val engangsstonadBelop: Int? = null
)

enum class VedtakStatus {
    INNVILGET,
    AVSLAG,
    ENGANGSSTONAD,
    MANUELL_VURDERING
}