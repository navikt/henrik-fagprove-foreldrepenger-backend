package no.nav.fagprove.foreldrepenger.service

import no.nav.fagprove.foreldrepenger.domain.Inntektsregistrering
import no.nav.fagprove.foreldrepenger.domain.Inntektstype
import no.nav.fagprove.foreldrepenger.domain.Soknad
import no.nav.fagprove.foreldrepenger.domain.VedtakStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDate

@SpringBootTest
class ForeldrepengerServiceTest {

    @Autowired
    lateinit var service: ForeldrepengerService

    @MockitoBean
    lateinit var vedtakLagringService: VedtakLagringService

    @MockitoBean
    lateinit var navSatserService: NavSatserService

    @Test
    fun `skal innvilge soknad naar alle vilkaar er oppfylt`() {
        mockSatser()

        val vedtak = service.behandleSoknad(
            gyldigSoknad(
                id = "innvilget",
                erMedlemIFolketrygden = true,
                inntektshistorikk = seksMaanederMedInntekt(),
                oppgittArsinntekt = 540000,
                rettsforhold = "begge",
                dekningsgrad = 100,
                antallBarn = 1,
            )
        )

        assertEquals(VedtakStatus.INNVILGET, vedtak.status)
        assertEquals(49, vedtak.totalStonadsperiodeUker)
        assertEquals(15, vedtak.kvoter?.modrekvoteUker)
        assertEquals(15, vedtak.kvoter?.fedrekvoteUker)
        assertEquals(16, vedtak.kvoter?.fellesperiodeUker)
        assertEquals(3, vedtak.kvoter?.forhandskvoteMorUker)
    }

    @Test
    fun `skal gi avslag naar søker ikke er medlem i folketrygden`() {
        mockSatser()

        val vedtak = service.behandleSoknad(
            gyldigSoknad(
                id = "avslag",
                erMedlemIFolketrygden = false,
                inntektshistorikk = seksMaanederMedInntekt(),
            )
        )

        assertEquals(VedtakStatus.AVSLAG, vedtak.status)
        assertEquals(null, vedtak.engangsstonadBelop)
        assertEquals(null, vedtak.totalStonadsperiodeUker)
    }

    @Test
    fun `skal gi engangsstonad naar opptjening ikke er oppfylt men søker er medlem`() {
        mockSatser()

        val vedtak = service.behandleSoknad(
            gyldigSoknad(
                id = "engangsstonad",
                erMedlemIFolketrygden = true,
                inntektshistorikk = femMaanederMedInntekt(),
            )
        )

        assertEquals(VedtakStatus.ENGANGSSTONAD, vedtak.status)
        assertEquals(92648, vedtak.engangsstonadBelop)
    }

    @Test
    fun `skal gi manuell vurdering ved mer enn 25 prosent avvik`() {
        mockSatser()

        val vedtak = service.behandleSoknad(
            gyldigSoknad(
                id = "manuell",
                erMedlemIFolketrygden = true,
                inntektshistorikk = seksMaanederMedInntekt(),
                oppgittArsinntekt = 100000,
            )
        )

        assertEquals(VedtakStatus.MANUELL_VURDERING, vedtak.status)
        assertEquals(true, vedtak.beregningsgrunnlag?.kreverManuellVurdering)
    }

    @Test
    fun `skal bruke 6G som maks begrensning for beregningsgrunnlag`() {
        mockSatser()

        val vedtak = service.behandleSoknad(
            gyldigSoknad(
                id = "seks-g",
                erMedlemIFolketrygden = true,
                inntektshistorikk = seksMaanederMedInntekt(belop = 100000),
                oppgittArsinntekt = 1200000,
            )
        )

        assertEquals(VedtakStatus.INNVILGET, vedtak.status)
        assertEquals(1200000, vedtak.beregningsgrunnlag?.arssats)
        assertEquals(780960, vedtak.beregningsgrunnlag?.begrensetTilSeksG)
    }

    @Test
    fun `skal bruke stonadsuker fra application yml`() {
        mockSatser()

        val vedtak = service.behandleSoknad(
            gyldigSoknad(
                id = "config",
                erMedlemIFolketrygden = true,
                inntektshistorikk = seksMaanederMedInntekt(),
                rettsforhold = "kun-far",
                dekningsgrad = 80,
                antallBarn = 1,
            )
        )

        assertEquals(VedtakStatus.INNVILGET, vedtak.status)
        assertEquals(52, vedtak.totalStonadsperiodeUker)
        assertEquals(0, vedtak.kvoter?.modrekvoteUker)
        assertEquals(52, vedtak.kvoter?.fedrekvoteUker)
        assertEquals(0, vedtak.kvoter?.forhandskvoteMorUker)
    }

    private fun mockSatser() {
        `when`(navSatserService.hentSatser()).thenReturn(
            NavSatser(
                grunnbeloep = 130160,
                halvG = 65080,
                seksG = 780960,
                engangsstonad = 92648,
                kilde = "TEST",
            )
        )
    }

    private fun gyldigSoknad(
        id: String,
        erMedlemIFolketrygden: Boolean = true,
        termindato: LocalDate = LocalDate.parse("2026-08-15"),
        oppgittArsinntekt: Int = 540000,
        inntektshistorikk: List<Inntektsregistrering> = seksMaanederMedInntekt(),
        antallBarn: Int = 1,
        rettsforhold: String = "begge",
        dekningsgrad: Int = 100,
    ): Soknad {
        return Soknad(
            id = id,
            beskrivelse = "Testsøknad",
            fnr = "04059012377",
            erMedlemIFolketrygden = erMedlemIFolketrygden,
            termindato = termindato,
            oppgittArsinntekt = oppgittArsinntekt,
            inntektshistorikk = inntektshistorikk,
            antallBarn = antallBarn,
            rettsforhold = rettsforhold,
            dekningsgrad = dekningsgrad,
        )
    }

    private fun seksMaanederMedInntekt(belop: Int = 45000): List<Inntektsregistrering> {
        return listOf(
            inntekt("2025-10", belop),
            inntekt("2025-11", belop),
            inntekt("2025-12", belop),
            inntekt("2026-01", belop),
            inntekt("2026-02", belop),
            inntekt("2026-03", belop),
        )
    }

    private fun femMaanederMedInntekt(): List<Inntektsregistrering> {
        return listOf(
            inntekt("2025-10", 45000),
            inntekt("2025-11", 45000),
            inntekt("2025-12", 45000),
            inntekt("2026-01", 45000),
            inntekt("2026-02", 45000),
        )
    }

    private fun inntekt(
        maned: String,
        belop: Int,
        type: Inntektstype = Inntektstype.ARBEID,
    ): Inntektsregistrering {
        return Inntektsregistrering(
            maned = maned,
            type = type,
            belop = belop,
        )
    }
}