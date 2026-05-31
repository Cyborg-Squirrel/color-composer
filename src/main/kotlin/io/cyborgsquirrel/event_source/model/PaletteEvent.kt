package io.cyborgsquirrel.event_source.model

import io.cyborgsquirrel.event_source.model.delta.PaletteDelta
import io.cyborgsquirrel.lighting.effect_palette.responses.GetPaletteResponse

sealed class PaletteEvent(uuid: String, data: Any?) : SseEvent(uuid, data) {
    class PaletteCreated(uuid: String, palette: GetPaletteResponse) : PaletteEvent(uuid, palette)
    class PaletteUpdated(uuid: String, delta: PaletteDelta) : PaletteEvent(uuid, delta)
    class PaletteDeleted(uuid: String) : PaletteEvent(uuid, null)
}
