package io.cyborgsquirrel.lighting.effects.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetEffectsResponse(val effects: List<GetEffectResponse>)
