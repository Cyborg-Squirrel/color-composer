package io.cyborgsquirrel.event_source

import io.cyborgsquirrel.event_source.model.*
import io.cyborgsquirrel.test_helpers.objectToMap
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest
class SseEventTest(
    private val objectMapper: ObjectMapper
) : StringSpec({

    fun uuid() = UUID.randomUUID().toString()

    "LightEffectCreated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LightEffectEvent.LightEffectCreated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LightEffectCreated"
    }

    "LightEffectUpdated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LightEffectEvent.LightEffectUpdated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LightEffectUpdated"
    }

    "LightEffectDeleted serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LightEffectEvent.LightEffectDeleted(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LightEffectDeleted"
    }

    "LedClientCreated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LedClientEvent.LedClientCreated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LedClientCreated"
    }

    "LedClientUpdated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LedClientEvent.LedClientUpdated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LedClientUpdated"
    }

    "LedClientDeleted serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LedClientEvent.LedClientDeleted(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LedClientDeleted"
    }

    "LedStripCreated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LedStripEvent.LedStripCreated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LedStripCreated"
    }

    "LedStripUpdated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LedStripEvent.LedStripUpdated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LedStripUpdated"
    }

    "LedStripDeleted serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, LedStripEvent.LedStripDeleted(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LedStripDeleted"
    }

    "StripPoolCreated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, StripPoolEvent.StripPoolCreated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "StripPoolCreated"
    }

    "StripPoolUpdated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, StripPoolEvent.StripPoolUpdated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "StripPoolUpdated"
    }

    "StripPoolDeleted serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, StripPoolEvent.StripPoolDeleted(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "StripPoolDeleted"
    }

    "PaletteCreated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, PaletteEvent.PaletteCreated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "PaletteCreated"
    }

    "PaletteUpdated serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, PaletteEvent.PaletteUpdated(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "PaletteUpdated"
    }

    "PaletteDeleted serializes uuid and type" {
        val id = uuid()
        val map = objectToMap(objectMapper, PaletteEvent.PaletteDeleted(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "PaletteDeleted"
    }
})
