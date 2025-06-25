package io.cyborgsquirrel.util

import io.micronaut.core.type.Argument
import io.micronaut.serde.*
import jakarta.inject.Singleton
import java.time.Duration

@Singleton
class DurationSerde : Serde<Duration> {
    override fun serialize(
        encoder: Encoder,
        context: Serializer.EncoderContext?,
        type: Argument<out Duration>?,
        value: Duration?
    ) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeLong(value.toMillis())
        }
    }

    override fun deserialize(
        decoder: Decoder,
        context: Deserializer.DecoderContext?,
        type: Argument<in Duration>?
    ): Duration? {
        val durationMillis = decoder.decodeLong()
        return Duration.ofMillis(durationMillis)
    }
}