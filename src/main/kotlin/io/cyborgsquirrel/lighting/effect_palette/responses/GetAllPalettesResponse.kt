package io.cyborgsquirrel.lighting.effect_palette.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetAllPalettesResponse(val palettes: List<GetPaletteResponse>)
