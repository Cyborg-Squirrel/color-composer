package io.cyborgsquirrel.lighting.effect_trigger

import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger

interface TriggerManager {
    /**
     * Adds a [LightEffectTrigger] for tracking
     */
    fun addTrigger(trigger: LightEffectTrigger)

    /**
     * Gets all current registered [LightEffectTrigger]s
     */
    fun getTriggers(): List<LightEffectTrigger>

    /**
     * Removes a trigger from the list of [LightEffectTrigger]s to track
     */
    fun removeTrigger(trigger: LightEffectTrigger)

    /**
     * Checks the next trigger activation.
     *
     * This will update the associated light effect in the
     * [io.cyborgsquirrel.lighting.effects.repository.ActiveLightEffectRepository] depending on the trigger's output
     */
    fun checkTriggerActivations()
}