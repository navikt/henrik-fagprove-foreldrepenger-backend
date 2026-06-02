package no.nav.fagprove.foreldrepenger.domain

import java.time.LocalDate

data class Soknad(
    val id: String,
    val beskrivelse: String? = null,
    val fnr: String,
    val erMedlemIFolketrygden: Boolean,
    val termindato: LocalDate,
    val oppgittArsinntekt: Int,
    val inntektshistorikk: List<Inntektsregistrering>,
    val antallBarn: Int,
    val rettsforhold: String,
    val dekningsgrad: Int
)