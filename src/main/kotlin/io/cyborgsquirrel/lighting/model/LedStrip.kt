package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

interface LedStrip {
    fun getName(): String

    fun getUuid(): String

    fun getPin(): String

    fun getLength(): Int

    fun getHeight(): Int

    fun getBlendMode(): BlendMode
}