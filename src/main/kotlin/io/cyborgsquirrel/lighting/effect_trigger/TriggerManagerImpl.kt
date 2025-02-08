package io.cyborgsquirrel.lighting.effect_trigger

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.triggers.EffectIterationTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.SunriseSunsetTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeTrigger
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.util.time.TimeHelper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class TriggerManagerImpl(
    private val effectRepository: ActiveLightEffectRegistry,
    private val timeHelper: TimeHelper
) : TriggerManager {

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

    override fun checkTriggerActivations() {
        val effects = effectRepository.findAllEffects()

        for (effect in effects) {
            val trigger = triggers.find { it.activeEffectUuid == effect.uuid }
            if (trigger != null) {
                val activationOptional = trigger.lastActivation()

                if (activationOptional.isPresent) {
                    val activation = activationOptional.get()
                    when (trigger) {
                        is EffectIterationTrigger -> {
                            // EffectIterationTrigger activation means the effect is running and should be paused.
                            // No need to check the trigger settings.
                            updateLightEffect(effect, TriggerType.StopEffect)
                        }

                        is SunriseSunsetTrigger -> {
                            checkSharedTriggerActivationConditions(trigger, activation, effect)
                        }

                        is TimeTrigger -> {
                            checkSharedTriggerActivationConditions(trigger, activation, effect)
                        }
                    }
                }
            } else {
                // TODO pause or remove trigger? Associated light effect no longer exists.
            }
        }
    }

    private fun checkSharedTriggerActivationConditions(
        trigger: LightEffectTrigger,
        activation: TriggerActivation,
        activeEffect: ActiveLightEffect
    ) {
        val settings = trigger.settings
        val now = timeHelper.now()
        val triggerTimeIsValid =
            activation.timestamp.plusSeconds(settings.activationDuration.seconds)
                .isAfter(now)
        val triggerHasReachedMaxActivations =
            settings.maxActivations != null && settings.maxActivations < activation.sequenceNumber
        if (triggerHasReachedMaxActivations) {
            // TODO if the effect was last activated by this trigger and is still active set it to inactive
            // then set the trigger to inactive
            updateLightEffect(activeEffect, TriggerType.StopEffect)
        } else if (triggerTimeIsValid) {
            updateLightEffect(activeEffect, settings.triggerType)
        } else {
            updateLightEffect(activeEffect, TriggerType.StopEffect)
        }
    }

    private fun updateLightEffect(effect: ActiveLightEffect, triggerType: TriggerType) {
        val newStatus = when (triggerType) {
            TriggerType.StartEffect -> LightEffectStatus.Playing
            TriggerType.StopEffect -> LightEffectStatus.Stopping
        }

        val stopTriggerNotNeeded = newStatus == LightEffectStatus.Stopping && effect.status == LightEffectStatus.Stopped
        val newStatusMatches = effect.status == newStatus
        if (stopTriggerNotNeeded || newStatusMatches) {
            return
        }

        logger.info("Updating effect ${effect.effect.getName()} to $newStatus")
        val newEffect = effect.copy(status = newStatus)
        effectRepository.addOrUpdateEffect(newEffect)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerManagerImpl::class.java)
    }
}