package no.nav.fagprove.foreldrepenger.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping("/hello")
    fun hello(): Map<String, String> =
        mapOf("message" to "Hello world from Spring Boot + Kotlin. Fagprøve foreldrepenger backend")
}