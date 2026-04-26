package io.cyborgsquirrel.home.responses

import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class HomeResponse(
    val clients: Int,
    val strips: Int,
    val effects: Int,
    val palettes: Int,
    val activeEffects: List<GetEffectResponse>,
)
