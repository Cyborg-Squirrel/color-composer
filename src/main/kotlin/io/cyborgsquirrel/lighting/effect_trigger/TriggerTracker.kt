package io.cyborgsquirrel.lighting.effect_trigger

import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger
import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import java.util.*

interface TriggerTracker {
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
     * Gets the next trigger activation.
     * @param trigger the [LightEffectTrigger] to check
     * @param sequenceNumber the sequence number indicating the latest [TriggerActivation] processed by the caller
     *
     * If the caller's sequence number is greater than or equal to the latest [TriggerActivation] sequence number an
     * empty [Optional] is returned because the latest activation was already processed by the caller.
     *
     * If the caller's sequence number is less than the latest [TriggerActivation] the latest activation is returned.
     *
     * If the [LightEffectTrigger]'s valid duration plus the trigger time is in the past, it is considered expired
     * and an empty [Optional] is returned.
     */
    fun getNextTriggerActivation(trigger: LightEffectTrigger, sequenceNumber: Short): Optional<TriggerActivation>
}