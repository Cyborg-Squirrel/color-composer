package io.cyborgsquirrel.lighting.effect_trigger.service

import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger

/**
 * Interface for managing and tracking light effect triggers.
 *
 * A `TriggerManager` is responsible for managing a collection of `LightEffectTrigger` objects,
 * which define the conditions for triggering changes to light effects. It provides methods to
 * add and remove triggers, retrieve the current list of triggers, and execute logic to check
 * if any triggers should activate based on their conditions.
 */
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
     * Processes any registered triggers.
     *
     * This will update the associated light effect in the
     * [io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService] depending on the trigger's output
     */
    fun processTriggers()
}