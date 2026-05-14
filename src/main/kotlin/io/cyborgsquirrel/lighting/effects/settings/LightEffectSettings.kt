package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.SettingsMetadata
import io.cyborgsquirrel.lighting.HasMetadata

sealed class LightEffectSettings(override val metadata: SettingsMetadata = SettingsMetadata()) :
    HasMetadata