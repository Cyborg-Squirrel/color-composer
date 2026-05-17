package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SparkleEffectSettings(
    val numDots: Int = 10,
    val fadeInMillisMax: Int = 10,
    val fadeInMillisMin: Int = 5,
    val fadeOutMillisMax: Int = 1000,
    val fadeOutMillisMin: Int = 150,
    val updatesPerSecond: Int = 30,
) : LightEffectSettings()
