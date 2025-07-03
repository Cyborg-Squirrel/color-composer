package io.cyborgsquirrel.led_strips.enums

import io.cyborgsquirrel.led_strips.enums.PiClientPin.entries

// Available Pi pins which support NeoPixel/WS2812
enum class PiClientPin(val pinName: String) {
    D10("D10"),
    D12("D12"),
    D18("D18"),
    D21("D21");

    companion object {
        private val pinMap = entries.associateBy { it.pinName }

        fun fromString(pin: String): PiClientPin {
            return pinMap[pin] ?: throw IllegalArgumentException("Undefined pin $pin")
        }

        fun isValid(pin: String) = pin in entries.associateBy { it.pinName }
    }
}
