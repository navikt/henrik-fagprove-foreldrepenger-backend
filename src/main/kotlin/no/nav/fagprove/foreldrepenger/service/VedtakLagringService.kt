package no.nav.fagprove.foreldrepenger.service

import no.nav.fagprove.foreldrepenger.database.VedtakEntity
import no.nav.fagprove.foreldrepenger.database.VedtakRepository
import no.nav.fagprove.foreldrepenger.domain.Vedtak
import org.springframework.stereotype.Service

@Service
class VedtakLagringService(
    private val vedtakRepository: VedtakRepository,
) {
    fun lagreVedtak(vedtak: Vedtak) {
        vedtakRepository.save(
            VedtakEntity(
                soknadId = vedtak.soknadId,
                status = vedtak.status.name,
                begrunnelse = vedtak.begrunnelse,

                totalStonadsperiodeUker = vedtak.totalStonadsperiodeUker,
                engangsstonadBelop = vedtak.engangsstonadBelop,

                beregningsgrunnlagArssats = vedtak.beregningsgrunnlag?.arssats,
                beregningsgrunnlagBegrensetTilSeksG = vedtak.beregningsgrunnlag?.begrensetTilSeksG,
                kreverManuellVurdering = vedtak.beregningsgrunnlag?.kreverManuellVurdering,

                modrekvoteUker = vedtak.kvoter?.modrekvoteUker,
                fedrekvoteUker = vedtak.kvoter?.fedrekvoteUker,
                fellesperiodeUker = vedtak.kvoter?.fellesperiodeUker,
                forhandskvoteMorUker = vedtak.kvoter?.forhandskvoteMorUker,
                flerbarnsbonusUker = vedtak.kvoter?.flerbarnsbonusUker,
            )
        )
    }

    fun hentAlleVedtak(): List<VedtakEntity> =
        vedtakRepository.findAll()

    fun hentVedtakForSoknad(soknadId: String): List<VedtakEntity> =
        vedtakRepository.findBySoknadId(soknadId)
}