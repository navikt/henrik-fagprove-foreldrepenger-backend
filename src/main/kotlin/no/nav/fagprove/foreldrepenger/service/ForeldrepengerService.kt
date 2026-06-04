package no.nav.fagprove.foreldrepenger.service

import no.nav.fagprove.foreldrepenger.config.BarnUkerConfig
import no.nav.fagprove.foreldrepenger.config.ForeldrepengerReglerProperties
import no.nav.fagprove.foreldrepenger.config.KvoteUkerConfig
import no.nav.fagprove.foreldrepenger.config.RettsforholdUkerConfig
import no.nav.fagprove.foreldrepenger.domain.Beregningsgrunnlag
import no.nav.fagprove.foreldrepenger.domain.Inntektsregistrering
import no.nav.fagprove.foreldrepenger.domain.Inntektstype
import no.nav.fagprove.foreldrepenger.domain.Kvoter
import no.nav.fagprove.foreldrepenger.domain.Soknad
import no.nav.fagprove.foreldrepenger.domain.Vedtak
import no.nav.fagprove.foreldrepenger.domain.VedtakStatus
import org.springframework.stereotype.Service
import kotlin.math.abs

@Service
class ForeldrepengerService(
    private val navSatserService: NavSatserService,
    private val regler: ForeldrepengerReglerProperties,
    private val vedtakLagringService: VedtakLagringService,
) {
    fun behandleSoknad(soknad: Soknad): Vedtak {
        val satser = navSatserService.hentSatser()

        if (!soknad.erMedlemIFolketrygden) {
            return lagreOgReturner(
                Vedtak(
                    soknadId = soknad.id,
                    status = VedtakStatus.AVSLAG,
                    begrunnelse = "Søker oppfyller ikke det forenklede medlemskravet i folketrygden og har derfor ikke rett til foreldrepenger eller engangsstønad."
                )
            )
        }

        val opptjeningOppfylt = harOpptjening(soknad)
        val arsinntektFraHistorikk = beregnArsinntektFraHistorikk(soknad)

        if (!opptjeningOppfylt || arsinntektFraHistorikk < satser.halvG) {
            return lagreOgReturner(
                Vedtak(
                    soknadId = soknad.id,
                    status = VedtakStatus.ENGANGSSTONAD,
                    begrunnelse = "Søker oppfyller ikke opptjeningskravet for foreldrepenger, men oppfyller det forenklede medlemskravet og får derfor engangsstønad.",
                    engangsstonadBelop = satser.engangsstonad
                )
            )
        }

        val beregningsgrunnlag = beregnBeregningsgrunnlag(soknad, satser)

        if (beregningsgrunnlag.kreverManuellVurdering) {
            return lagreOgReturner(
                Vedtak(
                    soknadId = soknad.id,
                    status = VedtakStatus.MANUELL_VURDERING,
                    begrunnelse = beregningsgrunnlag.begrunnelse
                        ?: "Søknaden krever manuell vurdering.",
                    beregningsgrunnlag = beregningsgrunnlag
                )
            )
        }

        val totalUker = beregnTotalStonadsperiodeUker(soknad)
        val kvoter = beregnKvoter(soknad, totalUker)

        return lagreOgReturner(
            Vedtak(
                soknadId = soknad.id,
                status = VedtakStatus.INNVILGET,
                begrunnelse = "Søker oppfyller vilkårene for foreldrepenger. Søknaden er innvilget med beregningsgrunnlag, stønadsperiode og kvoter.",
                beregningsgrunnlag = beregningsgrunnlag,
                totalStonadsperiodeUker = totalUker,
                kvoter = kvoter
            )
        )
    }

    private fun lagreOgReturner(vedtak: Vedtak): Vedtak {
        vedtakLagringService.lagreVedtak(vedtak)
        return vedtak
    }

    fun hentTestSoknader(): List<Soknad> {
        return listOf(
            Soknad(
                id = "fp-001-happy-path",
                beskrivelse = "Innvilget: søker oppfyller alle vilkår",
                fnr = "04059012377",
                erMedlemIFolketrygden = true,
                termindato = java.time.LocalDate.parse("2026-08-15"),
                oppgittArsinntekt = 540000,
                inntektshistorikk = listOf(
                    inntekt("2025-10", Inntektstype.ARBEID, 45000),
                    inntekt("2025-11", Inntektstype.ARBEID, 45000),
                    inntekt("2025-12", Inntektstype.ARBEID, 45000),
                    inntekt("2026-01", Inntektstype.ARBEID, 45000),
                    inntekt("2026-02", Inntektstype.ARBEID, 45000),
                    inntekt("2026-03", Inntektstype.ARBEID, 45000)
                ),
                antallBarn = 1,
                rettsforhold = "begge",
                dekningsgrad = 100
            )
        )
    }

    private fun harOpptjening(soknad: Soknad): Boolean {
        val godkjenteMaaneder = soknad.inntektshistorikk
            .filter { erGodkjentInntektstype(it.type) && it.belop > 0 }
            .map { it.maned }
            .distinct()
            .count()

        return godkjenteMaaneder >= 6
    }

    private fun erGodkjentInntektstype(type: Inntektstype): Boolean {
        return type != Inntektstype.STIPEND_LANEKASSEN
    }

    private fun beregnArsinntektFraHistorikk(soknad: Soknad): Int {
        if (soknad.inntektshistorikk.isEmpty()) return 0

        val totalGodkjentInntekt = soknad.inntektshistorikk
            .filter { erGodkjentInntektstype(it.type) }
            .sumOf { it.belop }

        return totalGodkjentInntekt * 12 / 10
    }

    private fun beregnBeregningsgrunnlag(
        soknad: Soknad,
        satser: NavSatser,
    ): Beregningsgrunnlag {
        val sisteTreMaaneder = soknad.inntektshistorikk
            .filter { erGodkjentInntektstype(it.type) }
            .takeLast(3)

        val snittTreMaaneder = if (sisteTreMaaneder.isEmpty()) {
            0
        } else {
            sisteTreMaaneder.sumOf { it.belop } / sisteTreMaaneder.size
        }

        val arssats = snittTreMaaneder * 12

        if (soknad.oppgittArsinntekt > 0) {
            val avvik = abs(arssats - soknad.oppgittArsinntekt).toDouble() / soknad.oppgittArsinntekt

            if (avvik > 0.25) {
                return Beregningsgrunnlag(
                    arssats = arssats,
                    begrensetTilSeksG = minOf(arssats, satser.seksG),
                    kreverManuellVurdering = true,
                    begrunnelse = "Søknaden krever manuell vurdering fordi det er mer enn 25 prosent avvik mellom beregnet årssats og oppgitt årsinntekt."
                )
            }
        }

        return Beregningsgrunnlag(
            arssats = arssats,
            begrensetTilSeksG = minOf(arssats, satser.seksG),
            kreverManuellVurdering = false
        )
    }

    private fun beregnTotalStonadsperiodeUker(soknad: Soknad): Int {
        val rettsforholdConfig: RettsforholdUkerConfig = when (soknad.rettsforhold) {
            "begge" -> regler.stonadsuker.begge
            "kun-mor" -> regler.stonadsuker.kunMor
            "kun-far" -> regler.stonadsuker.kunFar
            else -> throw IllegalArgumentException("Ugyldig rettsforhold: ${soknad.rettsforhold}")
        }

        val dekningsgradConfig: BarnUkerConfig = when (soknad.dekningsgrad) {
            100 -> rettsforholdConfig.hundre
            80 -> rettsforholdConfig.aatti
            else -> throw IllegalArgumentException("Ugyldig dekningsgrad: ${soknad.dekningsgrad}")
        }

        return when {
            soknad.antallBarn <= 1 -> dekningsgradConfig.ettBarn
            soknad.antallBarn == 2 -> dekningsgradConfig.toBarn
            else -> dekningsgradConfig.treEllerFlereBarn
        }
    }

    private fun beregnKvoter(soknad: Soknad, totalUker: Int): Kvoter {
        val kvoteConfig: KvoteUkerConfig = when (soknad.dekningsgrad) {
            100 -> regler.kvoter.hundre
            80 -> regler.kvoter.aatti
            else -> throw IllegalArgumentException("Ugyldig dekningsgrad: ${soknad.dekningsgrad}")
        }

        val flerbarnsbonus = beregnFlerbarnsbonus(soknad, kvoteConfig)
        val forhandskvoteMor = if (soknad.rettsforhold == "kun-far") {
            0
        } else {
            kvoteConfig.forhandskvoteMor
        }

        if (soknad.rettsforhold == "kun-mor") {
            return Kvoter(
                modrekvoteUker = totalUker - flerbarnsbonus,
                fedrekvoteUker = 0,
                fellesperiodeUker = 0,
                forhandskvoteMorUker = forhandskvoteMor,
                flerbarnsbonusUker = flerbarnsbonus
            )
        }

        if (soknad.rettsforhold == "kun-far") {
            return Kvoter(
                modrekvoteUker = 0,
                fedrekvoteUker = totalUker - flerbarnsbonus,
                fellesperiodeUker = 0,
                forhandskvoteMorUker = 0,
                flerbarnsbonusUker = flerbarnsbonus
            )
        }

        if (soknad.rettsforhold != "begge") {
            throw IllegalArgumentException("Ugyldig rettsforhold: ${soknad.rettsforhold}")
        }

        val modrekvote = kvoteConfig.modrekvote
        val fedrekvote = kvoteConfig.fedrekvote
        val fellesperiode = totalUker - modrekvote - fedrekvote - forhandskvoteMor - flerbarnsbonus

        return Kvoter(
            modrekvoteUker = modrekvote,
            fedrekvoteUker = fedrekvote,
            fellesperiodeUker = fellesperiode,
            forhandskvoteMorUker = forhandskvoteMor,
            flerbarnsbonusUker = flerbarnsbonus
        )
    }

    private fun beregnFlerbarnsbonus(
        soknad: Soknad,
        kvoteConfig: KvoteUkerConfig,
    ): Int {
        return when {
            soknad.antallBarn <= 1 -> 0
            soknad.antallBarn == 2 -> kvoteConfig.flerbarnsbonusToBarn
            else -> kvoteConfig.flerbarnsbonusTreEllerFlereBarn
        }
    }

    private fun inntekt(
        maned: String,
        type: Inntektstype,
        belop: Int
    ) = Inntektsregistrering(
        maned = maned,
        type = type,
        belop = belop
    )
}