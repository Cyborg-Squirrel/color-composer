package io.cyborgsquirrel.lighting.model

import kotlin.experimental.or

data class RgbFrameOptions(val byte: Byte) {
    companion object {
        fun blank(): RgbFrameOptions {
            return RgbFrameOptions(0)
        }
    }
}

class RgbFrameOptionsBuilder {
    var clearBuffer: Boolean = false
        private set

    fun setClearBuffer() {
        clearBuffer = true
    }

    fun build(): RgbFrameOptions {
        var optionsByte: Byte = 0x00
        optionsByte = optionsByte or 0x01
        return RgbFrameOptions(optionsByte)
    }
}