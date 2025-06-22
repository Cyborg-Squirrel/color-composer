package io.cyborgsquirrel.lighting.effect_palette.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreatePaletteRequest(
    val settings: Map<String, Any>,
    val name: String,
    val paletteType: String
)