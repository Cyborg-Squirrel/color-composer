package io.cyborgsquirrel.lighting.filters.settings

import io.cyborgsquirrel.lighting.SettingsMetadata
import io.cyborgsquirrel.lighting.HasMetadata

sealed class LightEffectFilterSettings(override val metadata: SettingsMetadata = SettingsMetadata()) : HasMetadata(metadata)
