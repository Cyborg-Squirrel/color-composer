package io.cyborgsquirrel.lighting.effect_trigger.model

import io.cyborgsquirrel.lighting.effect_trigger.settings.TriggerHasMetadata
import java.time.LocalDateTime

data class TriggerActivation(val timestamp: LocalDateTime, val settings: TriggerHasMetadata, val activationNumber: Int)