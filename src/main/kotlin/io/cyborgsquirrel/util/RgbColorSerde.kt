package io.cyborgsquirrel.util

import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.core.type.Argument
import io.micronaut.serde.*
import jakarta.inject.Singleton

@Singleton
class RgbColorSerde : Serde<RgbColor> {
    override fun serialize(
        encoder: Encoder,
        context: Serializer.EncoderContext?,
        type: Argument<out RgbColor>?,
        value: RgbColor?
    ) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val rgbHexString = String.format(
                "#%02X%02X%02X",
                value.red.toInt() and 0xFF,
                value.green.toInt() and 0xFF,
                value.blue.toInt() and 0xFF
            )
            encoder.encodeString(rgbHexString)
        }
    }

    override fun deserialize(
        decoder: Decoder,
        context: Deserializer.DecoderContext?,
        type: Argument<in RgbColor>?
    ): RgbColor? {
        val rgbHexString = decoder.decodeString()
        val rgbString = rgbHexString.removePrefix("#")
        if (rgbString.length < 6) {
            return null
        }
        val red = rgbString.substring(0, 2).toUByte(radix = 16)
        val green = rgbString.substring(2, 4).toUByte(radix = 16)
        val blue = rgbString.substring(4, 6).toUByte(radix = 16)

        return RgbColor(red, green, blue)
    }
}