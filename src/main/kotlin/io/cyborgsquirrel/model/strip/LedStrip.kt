package io.cyborgsquirrel.model.strip

import io.cyborgsquirrel.lighting.enums.BlendMode

interface LedStrip {
    fun getName(): String

    fun getUuid(): String

    fun getLength(): Int

    fun getHeight(): Int

    fun getBlendMode(): BlendMode
}