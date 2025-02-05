package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
class SunriseSunsetTriggerSettings(
    val sunriseSunsetOption: SunriseSunsetOption,
    activationDuration: Duration,
    maxActivations: Int?,
    triggerType: TriggerType
) :
    TriggerSettings(activationDuration, maxActivations, triggerType)