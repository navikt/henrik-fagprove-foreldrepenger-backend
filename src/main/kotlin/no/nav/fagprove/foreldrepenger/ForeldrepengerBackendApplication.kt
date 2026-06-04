package no.nav.fagprove.foreldrepenger

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ForeldrepengerBackendApplication

fun main(args: Array<String>) {
	runApplication<ForeldrepengerBackendApplication>(*args)
}