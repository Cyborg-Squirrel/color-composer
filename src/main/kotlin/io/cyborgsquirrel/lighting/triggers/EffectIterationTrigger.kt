package io.cyborgsquirrel.lighting.triggers

import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.enums.TriggerType
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class EffectIterationTrigger(
    private val effect: LightEffect,
    private val iterationsUntilTrigger: Int,
    triggerType: TriggerType
) :
    LightEffectTrigger(triggerType) {

    private var lastActivation: LocalDateTime? = null

    override fun init() {
        // Do nothing - intentionally left blank
    }

    override fun lastActivation(): Optional<LocalDateTime> {
        if (effect.getIterations() >= iterationsUntilTrigger) {
            if (lastActivation == null) {
                lastActivation = LocalDateTime.now()
            }

            return Optional.of(lastActivation!!)
        }

        return Optional.empty()
    }

    override fun triggerThreshold(): Duration {
        // Always trigger the effect if [lastActivation()] is non-empty
        return Duration.ofMillis(Long.MAX_VALUE)
    }
}