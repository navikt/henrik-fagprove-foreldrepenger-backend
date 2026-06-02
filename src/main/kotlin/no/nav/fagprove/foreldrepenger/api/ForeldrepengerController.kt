package no.nav.fagprove.foreldrepenger.api

import no.nav.fagprove.foreldrepenger.domain.Soknad
import no.nav.fagprove.foreldrepenger.domain.Vedtak
import no.nav.fagprove.foreldrepenger.service.ForeldrepengerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/foreldrepenger")
class ForeldrepengerController(
    private val foreldrepengerService: ForeldrepengerService
) {

    @GetMapping("/ping")
    fun ping(): Map<String, String> =
        mapOf(
            "status" to "ok",
            "message" to "Foreldrepenger-backend kjører"
        )

    @GetMapping("/soknader")
    fun hentTestSoknader(): List<Soknad> =
        foreldrepengerService.hentTestSoknader()

    @PostMapping("/behandle")
    fun behandleSoknad(@RequestBody soknad: Soknad): Vedtak =
        foreldrepengerService.behandleSoknad(soknad)
}