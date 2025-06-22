package io.cyborgsquirrel.lighting.effect_palette.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetPaletteResponse(
    val name: String,
    val uuid: String,
    val type: String,
    val settings: Map<String, Any>
)