package no.nav.fagprove.foreldrepenger.database

import org.springframework.data.jpa.repository.JpaRepository

interface VedtakRepository : JpaRepository<VedtakEntity, Long> {
    fun findBySoknadId(soknadId: String): List<VedtakEntity>
}