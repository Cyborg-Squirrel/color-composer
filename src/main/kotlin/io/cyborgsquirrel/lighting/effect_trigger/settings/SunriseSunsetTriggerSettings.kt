package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import java.time.Duration

class SunriseSunsetTriggerSettings(
    val sunriseSunsetOption: SunriseSunsetOption,
    activationDuration: Duration,
    maxActivations: Int?,
    type: TriggerType
) :
    TriggerSettings(activationDuration, maxActivations, type)