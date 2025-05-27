package io.cyborgsquirrel.lighting.effects.responses

data class GetActiveEffectResponse(
    val name: String,
    val effectUuid: String,
)
