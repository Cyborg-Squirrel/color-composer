package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.settings.EffectIterationTriggerSettings
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.util.time.TimeHelper
import java.time.LocalDateTime
import java.util.*

/**
 * [LightEffectTrigger] which activates when the [LightEffect] reaches a configured number of iterations
 * This trigger is best used when the effect is playing and is configured to deactivate after a specified number of iterations
 */
class EffectIterationTrigger(
    private val timeHelper: TimeHelper,
    private val effectRegistry: ActiveLightEffectRegistry,
    settings: EffectIterationTriggerSettings,
    uuid: String,
    effectUuid: String,
) :
    LightEffectTrigger(settings, uuid, effectUuid) {

    private var lastActivation: LocalDateTime? = null
    private var sequenceNumber = 0

    override fun lastActivation(): Optional<TriggerActivation> {
        val effectOptional = effectRegistry.getEffectWithUuid(effectUuid)
        if (effectOptional.isPresent) {
            val activeEffect = effectOptional.get()
            if (activeEffect.effect.getIterations() > getMaxIterations() && lastActivation == null) {
                sequenceNumber++
                lastActivation = timeHelper.now()
            }

            return if (lastActivation == null) Optional.empty() else Optional.of(
                TriggerActivation(
                    lastActivation!!,
                    settings,
                    sequenceNumber,
                )
            )
        } else {
            // Couldn't find the light effect - trigger is desynced from active light effects
            return Optional.empty()
        }
    }

    private fun getMaxIterations(): Int {
        return (settings as EffectIterationTriggerSettings).maxActivations!!
    }
}