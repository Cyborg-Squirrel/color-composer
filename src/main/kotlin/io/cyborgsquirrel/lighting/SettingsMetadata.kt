package io.cyborgsquirrel.lighting

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SettingsMetadata(val majorVersion: Int = 1, val minorVersion: Int = 0)
