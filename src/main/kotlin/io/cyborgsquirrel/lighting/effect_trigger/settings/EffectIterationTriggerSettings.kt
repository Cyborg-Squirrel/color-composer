package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import java.time.Duration
import java.time.LocalTime

class EffectIterationTriggerSettings(
    validDuration: Duration?,
    maxActivations: Int,
    type: TriggerType
) :
    TriggerSettings(validDuration, maxActivations, type)