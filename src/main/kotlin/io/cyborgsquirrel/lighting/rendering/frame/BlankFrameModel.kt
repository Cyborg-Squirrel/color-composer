package io.cyborgsquirrel.lighting.rendering.frame

class BlankFrameModel(
    lightUuid: String,
) : RenderedFrame(0, lightUuid, listOf(), INVALID_SEQUENCE_NUMBER) {
    companion object {
        const val INVALID_SEQUENCE_NUMBER: Short = -1
    }
}
