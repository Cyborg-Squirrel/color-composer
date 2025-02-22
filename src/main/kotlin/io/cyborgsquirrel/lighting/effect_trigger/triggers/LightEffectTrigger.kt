package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.settings.TriggerSettings
import java.util.*

sealed class LightEffectTrigger(val settings: TriggerSettings, val uuid: String, val effectUuid: String) {

    /**
     * Checks the trigger condition and returns the most recent activation of the trigger.
     * If the trigger condition has not been met the [Optional] object will be empty.
     */
    abstract fun lastActivation(): Optional<TriggerActivation>
}