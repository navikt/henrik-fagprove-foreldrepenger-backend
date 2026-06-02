package no.nav.fagprove.foreldrepenger.service

import no.nav.fagprove.foreldrepenger.domain.Beregningsgrunnlag
import no.nav.fagprove.foreldrepenger.domain.Kvoter
import no.nav.fagprove.foreldrepenger.domain.Soknad
import no.nav.fagprove.foreldrepenger.domain.Vedtak
import no.nav.fagprove.foreldrepenger.domain.VedtakStatus
import org.springframework.stereotype.Service

@Service
class ForeldrepengerService {

    fun behandleSoknad(soknad: Soknad): Vedtak {
        if (!soknad.erMedlemIFolketrygden) {
            return Vedtak(
                soknadId = soknad.id,
                status = VedtakStatus.AVSLAG,
                begrunnelse = "Søker oppfyller ikke det forenklede medlemskravet i folketrygden og får derfor avslag.",
                beregningsgrunnlag = null,
                totalStonadsperiodeUker = null,
                kvoter = null,
                engangsstonadBelop = null
            )
        }

        return Vedtak(
            soknadId = soknad.id,
            status = VedtakStatus.INNVILGET,
            begrunnelse = "Søknaden er mottatt og behandlet med midlertidig dag-2-logikk. Full regelmotor implementeres på dag 3.",
            beregningsgrunnlag = Beregningsgrunnlag(
                arssats = soknad.oppgittArsinntekt,
                begrensetTilSeksG = minOf(soknad.oppgittArsinntekt, 780_960),
                kreverManuellVurdering = false
            ),
            totalStonadsperiodeUker = 49,
            kvoter = Kvoter(
                modrekvoteUker = 15,
                fedrekvoteUker = 15,
                fellesperiodeUker = 16,
                forhandskvoteMorUker = 3,
                flerbarnsbonusUker = 0
            )
        )
    }

    fun hentTestSoknader(): List<Soknad> {
        return listOf(
            Soknad(
                id = "fp-001-happy-path",
                beskrivelse = "Testdata: søker oppfyller vilkår for foreldrepenger",
                fnr = "04059012377",
                erMedlemIFolketrygden = true,
                termindato = java.time.LocalDate.parse("2026-08-15"),
                oppgittArsinntekt = 540000,
                inntektshistorikk = emptyList(),
                antallBarn = 1,
                rettsforhold = "begge",
                dekningsgrad = 100
            )
        )
    }
}