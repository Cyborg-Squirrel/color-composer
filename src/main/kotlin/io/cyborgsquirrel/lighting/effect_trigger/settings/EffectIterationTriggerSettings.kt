package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import java.time.Duration
import java.time.LocalTime

class EffectIterationTriggerSettings : TriggerSettings(Duration.ofSeconds(0), 1, TriggerType.StopEffect)