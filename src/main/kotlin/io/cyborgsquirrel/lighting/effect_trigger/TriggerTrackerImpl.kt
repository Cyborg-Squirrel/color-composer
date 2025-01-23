package io.cyborgsquirrel.lighting.effect_trigger

import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger
import jakarta.inject.Singleton
import java.util.*

@Singleton
class TriggerTrackerImpl : TriggerTracker {

    private val triggers = mutableListOf<LightEffectTrigger>()

    override fun addTrigger(trigger: LightEffectTrigger) {
        if (!triggers.contains(trigger)) {
            triggers.add(trigger)
        }
    }

    override fun getTriggers(): List<LightEffectTrigger> {
        return triggers
    }

    override fun removeTrigger(trigger: LightEffectTrigger) {
        triggers.remove(trigger)
    }

    override fun getNextTriggerActivation(
        trigger: LightEffectTrigger,
        sequenceNumber: Short
    ): Optional<TriggerActivation> {
        val trackedTrigger = triggers.find { it == trigger } ?: return Optional.empty()
        val activationOptional = trackedTrigger.lastActivation()

        if (activationOptional.isPresent) {
            val activation = activationOptional.get()
            val sequenceNumberLessThanTrigger = sequenceNumber < activation.sequenceNumber
            if (sequenceNumberLessThanTrigger) {
                return activationOptional
            }
        }

        return Optional.empty()
    }
}