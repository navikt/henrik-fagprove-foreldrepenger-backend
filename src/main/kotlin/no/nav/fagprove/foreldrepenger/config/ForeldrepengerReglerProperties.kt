package no.nav.fagprove.foreldrepenger.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "foreldrepenger.regler")
data class ForeldrepengerReglerProperties(
    val grunnbelopFallback: Int,
    val engangsstonadFallback: Int,
    val stonadsuker: StonadsukerConfig,
    val kvoter: KvoterConfig,
)

data class StonadsukerConfig(
    val begge: RettsforholdUkerConfig,
    val kunMor: RettsforholdUkerConfig,
    val kunFar: RettsforholdUkerConfig,
)

data class RettsforholdUkerConfig(
    val hundre: BarnUkerConfig,
    val aatti: BarnUkerConfig,
)

data class BarnUkerConfig(
    val ettBarn: Int,
    val toBarn: Int,
    val treEllerFlereBarn: Int,
)

data class KvoterConfig(
    val hundre: KvoteUkerConfig,
    val aatti: KvoteUkerConfig,
)

data class KvoteUkerConfig(
    val modrekvote: Int,
    val fedrekvote: Int,
    val forhandskvoteMor: Int,
    val flerbarnsbonusToBarn: Int,
    val flerbarnsbonusTreEllerFlereBarn: Int,
)