package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class EffectResponseSerializationTest(
    private val objectMapper: ObjectMapper
) : StringSpec({

    "GetStripEffectResponse serialization" {
        val originalResponse = GetStripEffectResponse(
            name = "Test Strip Effect",
            type = "Nightrider",
            uuid = UUID.randomUUID().toString(),
            stripUuid = UUID.randomUUID().toString(),
            paletteUuid = UUID.randomUUID().toString(),
            settingsUuid = null,
            status = LightEffectStatus.Playing,
            category = EffectCategory.Motion,
        )

        val json = objectMapper.writeValueAsString(originalResponse)
        val deserializedResponse = objectMapper.readValue(json, GetStripEffectResponse::class.java)
        deserializedResponse shouldNotBe null
        deserializedResponse.name shouldBe originalResponse.name
        deserializedResponse.type shouldBe originalResponse.type
        deserializedResponse.stripUuid shouldBe originalResponse.stripUuid
        deserializedResponse.uuid shouldBe originalResponse.uuid
        deserializedResponse.status shouldBe originalResponse.status
        deserializedResponse.paletteUuid shouldBe originalResponse.paletteUuid
        deserializedResponse.category shouldBe originalResponse.category
    }

    "GetPoolEffectResponse serialization" {
        val originalResponse = GetPoolEffectResponse(
            name = "Test Pool Effect",
            type = "Rainbow",
            uuid = UUID.randomUUID().toString(),
            poolUuid = UUID.randomUUID().toString(),
            paletteUuid = null,
            settingsUuid = null,
            status = LightEffectStatus.Paused,
            category = EffectCategory.Ambient,
        )

        val json = objectMapper.writeValueAsString(originalResponse)
        val deserializedResponse = objectMapper.readValue(json, GetPoolEffectResponse::class.java)
        deserializedResponse shouldNotBe null
        deserializedResponse.name shouldBe originalResponse.name
        deserializedResponse.type shouldBe originalResponse.type
        deserializedResponse.poolUuid shouldBe originalResponse.poolUuid
        deserializedResponse.uuid shouldBe originalResponse.uuid
        deserializedResponse.status shouldBe originalResponse.status
        deserializedResponse.category shouldBe originalResponse.category
    }

    "GetEffectsResponse serialization" {
        val stripEffect = GetStripEffectResponse(
            name = "Effect 1",
            type = "Type1",
            uuid = UUID.randomUUID().toString(),
            stripUuid = UUID.randomUUID().toString(),
            paletteUuid = null,
            settingsUuid = null,
            status = LightEffectStatus.Idle,
            category = EffectCategory.Static,
        )

        val poolEffect = GetPoolEffectResponse(
            name = "Effect 2",
            type = "Type2",
            uuid = UUID.randomUUID().toString(),
            poolUuid = UUID.randomUUID().toString(),
            paletteUuid = null,
            settingsUuid = null,
            status = LightEffectStatus.Playing,
            category = EffectCategory.Motion,
        )

        val originalResponse = GetEffectsResponse(
            effects = listOf(stripEffect, poolEffect)
        )

        // Test serialization doesn't crash
        objectMapper.writeValueAsString(originalResponse)
    }

    "GetStripEffectResponse handles null palette UUID" {
        val responseWithoutPalette = GetStripEffectResponse(
            name = "Effect without palette",
            type = "SolidColor",
            uuid = UUID.randomUUID().toString(),
            stripUuid = UUID.randomUUID().toString(),
            paletteUuid = null,
            settingsUuid = null,
            status = LightEffectStatus.Stopped,
            category = EffectCategory.Static,
        )

        val json = objectMapper.writeValueAsString(responseWithoutPalette)
        val deserialized = objectMapper.readValue(json, GetStripEffectResponse::class.java)
        deserialized.paletteUuid shouldBe null
    }

    "GetPoolEffectResponse handles null strip UUID" {
        val responseWithoutStrip = GetPoolEffectResponse(
            name = "Pool effect",
            type = "Gradient",
            uuid = UUID.randomUUID().toString(),
            poolUuid = UUID.randomUUID().toString(),
            paletteUuid = UUID.randomUUID().toString(),
            settingsUuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Playing,
            category = EffectCategory.Ambient,
        )

        val json = objectMapper.writeValueAsString(responseWithoutStrip)
        val deserialized = objectMapper.readValue(json, GetPoolEffectResponse::class.java)
        deserialized.poolUuid shouldBe responseWithoutStrip.poolUuid
    }
})
