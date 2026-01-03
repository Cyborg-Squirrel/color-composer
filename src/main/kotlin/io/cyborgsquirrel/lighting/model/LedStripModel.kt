package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

sealed class LedStripModel(
    open val uuid: String,
    open val name: String,
    open val blendMode: BlendMode
) {
    abstract fun length(): Int
}