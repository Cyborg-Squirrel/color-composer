package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.settings.EffectIterationTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.settings.TriggerSettings
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelper
import java.time.LocalDateTime
import java.util.*

/**
 * [LightEffectTrigger] which activates when the [LightEffect] reaches a configured number of iterations
 * This trigger is best used when the effect is playing and is configured to deactivate after a specified number of iterations
 */
class EffectIterationTrigger(
    private val effect: LightEffect,
    private val timeHelper: TimeHelper,
    settings: EffectIterationTriggerSettings
) :
    LightEffectTrigger(settings) {

    private var lastActivation: LocalDateTime? = null
    private var sequenceNumber = 0

    override fun lastActivation(): Optional<TriggerActivation> {
        if (effect.getIterations() > getMaxIterations() && lastActivation == null) {
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
    }

    private fun getMaxIterations() : Int {
        return (settings as EffectIterationTriggerSettings).maxActivations!!
    }
}