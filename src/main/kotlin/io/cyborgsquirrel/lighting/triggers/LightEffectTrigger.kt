package io.cyborgsquirrel.lighting.triggers

import io.cyborgsquirrel.lighting.enums.TriggerType
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

abstract class LightEffectTrigger(val triggerType: TriggerType) {

    /**
     * Initializes the trigger
     */
    abstract fun init()

    /**
     * Checks the trigger condition and returns the most recent activation of the trigger.
     * If the trigger condition has not been met the [Optional] object will be empty.
     */
    abstract fun lastActivation(): Optional<LocalDateTime>

    /**
     * Gets the threshold duration that the [lastActivation] should be considered valid.
     * Example: [lastActivation] is 10 minutes ago but the threshold is 5 minutes, the effect should not be triggered.
     */
    abstract fun triggerThreshold(): Duration
}