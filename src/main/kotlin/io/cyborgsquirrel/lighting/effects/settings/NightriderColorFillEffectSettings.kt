package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderColorFillEffectSettings(
    val wrap: Boolean = false,
) : NightriderEffectSettings() {

    override fun wrap(): Boolean {
        return wrap
    }
}