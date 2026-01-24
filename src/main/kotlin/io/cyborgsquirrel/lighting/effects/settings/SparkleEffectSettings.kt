package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SparkleEffectSettings(
    val numDots: Int,
    val fadeInMillisMax: Int,
    val fadeInMillisMin: Int,
    val fadeOutMillisMax: Int,
    val fadeOutMillisMin: Int
) : LightEffectSettings() {
    companion object {
        fun default() = SparkleEffectSettings(
            numDots = 10,
            fadeInMillisMax = 10,
            fadeInMillisMin = 5,
            fadeOutMillisMax = 1000,
            fadeOutMillisMin = 150,
        )
    }
}
