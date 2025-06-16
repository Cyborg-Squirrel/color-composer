package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
class TimeOfDayTriggerSettings(
    val timeOfDay: TimeOfDay,
    activationDuration: Duration,
    maxActivations: Int?,
    triggerType: TriggerType
) :
    TriggerSettings(activationDuration, maxActivations, triggerType)