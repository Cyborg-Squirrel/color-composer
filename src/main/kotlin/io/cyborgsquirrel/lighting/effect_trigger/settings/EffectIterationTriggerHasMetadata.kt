package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
class EffectIterationTriggerHasMetadata(maxActivations: Int) :
    TriggerHasMetadata(Duration.ofSeconds(0), maxActivations, TriggerType.StopEffect)