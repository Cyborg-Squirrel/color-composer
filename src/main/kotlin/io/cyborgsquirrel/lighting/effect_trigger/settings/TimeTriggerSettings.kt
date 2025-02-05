package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration
import java.time.LocalTime

@Serdeable
class TimeTriggerSettings(
    val triggerTime: LocalTime,
    activationDuration: Duration,
    maxActivations: Int?,
    triggerType: TriggerType
) :
    TriggerSettings(activationDuration, maxActivations, triggerType)