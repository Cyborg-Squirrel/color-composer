package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@Serdeable
class TimeTriggerHasMetadata(
    val triggerTime: LocalTime,
    val triggerDay: LocalDate?,
    activationDuration: Duration,
    maxActivations: Int?,
    triggerType: TriggerType,
) :
    TriggerHasMetadata(activationDuration, maxActivations, triggerType)