package io.cyborgsquirrel.event_source.model

sealed class PaletteEvent(uuid: String) : SseEvent(uuid) {
    class PaletteCreated(uuid: String) : PaletteEvent(uuid)
    class PaletteUpdated(uuid: String) : PaletteEvent(uuid)
    class PaletteDeleted(uuid: String) : PaletteEvent(uuid)
}
