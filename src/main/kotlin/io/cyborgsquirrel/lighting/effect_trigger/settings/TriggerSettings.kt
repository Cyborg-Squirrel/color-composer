package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import java.time.Duration

open class TriggerSettings(val activationDuration: Duration, val maxActivations: Int?, val triggerType: TriggerType)