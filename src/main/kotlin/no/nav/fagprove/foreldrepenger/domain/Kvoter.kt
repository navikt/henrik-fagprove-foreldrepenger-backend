package no.nav.fagprove.foreldrepenger.domain

data class Kvoter(
    val modrekvoteUker: Int,
    val fedrekvoteUker: Int,
    val fellesperiodeUker: Int,
    val forhandskvoteMorUker: Int,
    val flerbarnsbonusUker: Int
)