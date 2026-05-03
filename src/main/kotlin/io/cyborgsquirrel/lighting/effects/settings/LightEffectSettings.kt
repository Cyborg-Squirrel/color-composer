package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.VersionedSettings

sealed class LightEffectSettings(override val majorVersion: Int = 1, override val minorVersion: Int = 0) : VersionedSettings