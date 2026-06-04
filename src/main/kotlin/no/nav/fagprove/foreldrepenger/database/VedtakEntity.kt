package no.nav.fagprove.foreldrepenger.database

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class VedtakEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    var soknadId: String = "",

    var status: String = "",

    @Column(length = 2000)
    var begrunnelse: String = "",

    var totalStonadsperiodeUker: Int? = null,

    var engangsstonadBelop: Int? = null,

    var beregningsgrunnlagArssats: Int? = null,

    var beregningsgrunnlagBegrensetTilSeksG: Int? = null,

    var kreverManuellVurdering: Boolean? = null,

    var modrekvoteUker: Int? = null,

    var fedrekvoteUker: Int? = null,

    var fellesperiodeUker: Int? = null,

    var forhandskvoteMorUker: Int? = null,

    var flerbarnsbonusUker: Int? = null,

    var opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)