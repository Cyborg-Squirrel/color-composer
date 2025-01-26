package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import java.time.Duration
import java.time.LocalTime

class TimeTriggerSettings(
    val triggerTime: LocalTime,
    activationDuration: Duration,
    maxActivations: Int?,
    type: TriggerType
) :
    TriggerSettings(activationDuration, maxActivations, type)