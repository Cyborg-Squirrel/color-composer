package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

sealed class LedStripModel {
    abstract fun uuid(): String

    abstract fun name(): String

    abstract fun length(): Int

    abstract fun blendMode(): BlendMode
}