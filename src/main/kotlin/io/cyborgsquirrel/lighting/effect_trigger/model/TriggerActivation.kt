package io.cyborgsquirrel.lighting.effect_trigger.model

import io.cyborgsquirrel.lighting.effect_trigger.settings.TriggerSettings
import java.time.LocalDateTime

data class TriggerActivation(val timestamp: LocalDateTime, val settings: TriggerSettings, val activationNumber: Int)