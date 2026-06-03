package no.nav.fagprove.foreldrepenger.service

import no.nav.fagprove.foreldrepenger.domain.Beregningsgrunnlag
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
) {
    fun behandleSoknad(soknad: Soknad): Vedtak {
        val satser = navSatserService.hentSatser()

        if (!soknad.erMedlemIFolketrygden) {
            return Vedtak(
                soknadId = soknad.id,
                status = VedtakStatus.AVSLAG,
                begrunnelse = "Søker oppfyller ikke det forenklede medlemskravet i folketrygden og har derfor ikke rett til foreldrepenger eller engangsstønad."
            )
        }

        val opptjeningOppfylt = harOpptjening(soknad)
        val arsinntektFraHistorikk = beregnArsinntektFraHistorikk(soknad)

        if (!opptjeningOppfylt || arsinntektFraHistorikk < satser.halvG) {
            return Vedtak(
                soknadId = soknad.id,
                status = VedtakStatus.ENGANGSSTONAD,
                begrunnelse = "Søker oppfyller ikke opptjeningskravet for foreldrepenger, men oppfyller det forenklede medlemskravet og får derfor engangsstønad.",
                engangsstonadBelop = satser.engangsstonad
            )
        }

        val beregningsgrunnlag = beregnBeregningsgrunnlag(soknad, satser)

        if (beregningsgrunnlag.kreverManuellVurdering) {
            return Vedtak(
                soknadId = soknad.id,
                status = VedtakStatus.MANUELL_VURDERING,
                begrunnelse = beregningsgrunnlag.begrunnelse
                    ?: "Søknaden krever manuell vurdering.",
                beregningsgrunnlag = beregningsgrunnlag
            )
        }

        val totalUker = beregnTotalStonadsperiodeUker(soknad)
        val kvoter = beregnKvoter(soknad, totalUker)

        return Vedtak(
            soknadId = soknad.id,
            status = VedtakStatus.INNVILGET,
            begrunnelse = "Søker oppfyller vilkårene for foreldrepenger. Søknaden er innvilget med beregningsgrunnlag, stønadsperiode og kvoter.",
            beregningsgrunnlag = beregningsgrunnlag,
            totalStonadsperiodeUker = totalUker,
            kvoter = kvoter
        )
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
        val barnKategori = when {
            soknad.antallBarn <= 1 -> 1
            soknad.antallBarn == 2 -> 2
            else -> 3
        }

        return when (soknad.rettsforhold) {
            "begge" -> when (barnKategori) {
                1 -> if (soknad.dekningsgrad == 100) 49 else 61
                2 -> if (soknad.dekningsgrad == 100) 66 else 82
                else -> if (soknad.dekningsgrad == 100) 95 else 118
            }

            "kun-mor" -> when (barnKategori) {
                1 -> if (soknad.dekningsgrad == 100) 49 else 61
                2 -> if (soknad.dekningsgrad == 100) 66 else 82
                else -> if (soknad.dekningsgrad == 100) 95 else 118
            }

            "kun-far" -> when (barnKategori) {
                1 -> if (soknad.dekningsgrad == 100) 40 else 52
                2 -> if (soknad.dekningsgrad == 100) 57 else 73
                else -> if (soknad.dekningsgrad == 100) 86 else 109
            }

            else -> throw IllegalArgumentException("Ugyldig rettsforhold: ${soknad.rettsforhold}")
        }
    }

    private fun beregnKvoter(soknad: Soknad, totalUker: Int): Kvoter {
        val flerbarnsbonus = beregnFlerbarnsbonus(soknad)
        val forhandskvoteMor = if (soknad.rettsforhold == "kun-far") 0 else 3

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

        val modrekvote = if (soknad.dekningsgrad == 100) 15 else 19
        val fedrekvote = if (soknad.dekningsgrad == 100) 15 else 19
        val fellesperiode = totalUker - modrekvote - fedrekvote - forhandskvoteMor - flerbarnsbonus

        return Kvoter(
            modrekvoteUker = modrekvote,
            fedrekvoteUker = fedrekvote,
            fellesperiodeUker = fellesperiode,
            forhandskvoteMorUker = forhandskvoteMor,
            flerbarnsbonusUker = flerbarnsbonus
        )
    }

    private fun beregnFlerbarnsbonus(soknad: Soknad): Int {
        if (soknad.antallBarn <= 1) return 0

        return if (soknad.dekningsgrad == 100) {
            when (soknad.antallBarn) {
                2 -> 17
                else -> 46
            }
        } else {
            when (soknad.antallBarn) {
                2 -> 21
                else -> 57
            }
        }
    }

    private fun inntekt(
        maned: String,
        type: Inntektstype,
        belop: Int
    ) = no.nav.fagprove.foreldrepenger.domain.Inntektsregistrering(
        maned = maned,
        type = type,
        belop = belop
    )
}