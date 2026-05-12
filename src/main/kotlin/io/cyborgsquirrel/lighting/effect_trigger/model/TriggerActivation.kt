package io.cyborgsquirrel.lighting.effect_trigger.model

import io.cyborgsquirrel.lighting.effect_trigger.settings.Trigger
import java.time.LocalDateTime

data class TriggerActivation(val timestamp: LocalDateTime, val settings: Trigger, val activationNumber: Int)