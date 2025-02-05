package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
open class TriggerSettings(val activationDuration: Duration, val maxActivations: Int?, val triggerType: TriggerType)