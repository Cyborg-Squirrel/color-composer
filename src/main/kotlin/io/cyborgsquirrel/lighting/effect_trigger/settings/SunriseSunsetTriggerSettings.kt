package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import java.time.Duration

class SunriseSunsetTriggerSettings(
    val sunriseSunsetOption: SunriseSunsetOption,
    validDuration: Duration?,
    maxActivations: Int?,
    type: TriggerType
) :
    TriggerSettings(validDuration, maxActivations, type)