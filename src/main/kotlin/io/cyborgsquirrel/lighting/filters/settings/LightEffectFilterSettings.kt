package io.cyborgsquirrel.lighting.filters.settings

import io.cyborgsquirrel.lighting.VersionedSettings

sealed class LightEffectFilterSettings(override val majorVersion: Int = 1, override val minorVersion: Int = 0) : VersionedSettings
