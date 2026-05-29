package io.cyborgsquirrel.event_source

import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.event_source.model.*
import io.cyborgsquirrel.event_source.model.delta.ClientDelta
import io.cyborgsquirrel.event_source.model.delta.EffectDelta
import io.cyborgsquirrel.event_source.model.delta.EffectSettingsDelta
import io.cyborgsquirrel.event_source.model.delta.PaletteDelta
import io.cyborgsquirrel.event_source.model.delta.PoolDelta
import io.cyborgsquirrel.event_source.model.delta.StripDelta
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.responses.GetLedStripResponse
import io.cyborgsquirrel.lighting.effect_palette.responses.GetPaletteResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectSettingsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolResponse
import io.cyborgsquirrel.strip_pools.responses.StripPoolMemberResponseModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest
class SseEventTest(
    private val objectMapper: ObjectMapper
) : StringSpec({

    fun uuid() = UUID.randomUUID().toString()

    // Serializes through the runtime type (as the SSE controller does) and re-reads the JSON into a
    // nested map so the `data` payload can be inspected.
    fun toMap(event: SseEvent): Map<String, Any> {
        val json = objectMapper.writeValueAsString(event)
        @Suppress("UNCHECKED_CAST")
        return objectMapper.readValue(json, Map::class.java) as Map<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    fun Map<String, Any>.data() = this["data"] as Map<String, Any>

    // JSON integers may round-trip as Int or Long depending on the decoder; compare numerically.
    fun Map<String, Any>.intField(key: String) = (this[key] as Number).toInt()

    // region Created events carry the full GET-API object

    "LightEffectCreated carries the effect object" {
        val id = uuid()
        val effect = GetStripEffectResponse(
            name = "My Effect",
            type = "Flame",
            uuid = id,
            stripUuid = "strip-1",
            paletteUuid = "palette-1",
            settingsUuid = "settings-1",
            status = LightEffectStatus.Playing,
            category = EffectCategory.Ambient,
            layer = 2,
        )
        val map = toMap(LightEffectEvent.LightEffectCreated(id, effect))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LightEffectCreated"
        val data = map.data()
        data["uuid"] shouldBe id
        data["name"] shouldBe "My Effect"
        data["stripUuid"] shouldBe "strip-1"
        data.intField("layer") shouldBe 2
        data["status"] shouldBe "Playing"
    }

    "LedClientCreated carries the client object" {
        val id = uuid()
        val client = GetClientResponse(
            name = "Living Room",
            address = "10.0.0.5",
            uuid = id,
            clientType = "Pi",
            colorOrder = ColorOrder.GRB,
            apiPort = 8080,
            wsPort = 81,
            lastSeenAt = 123L,
            status = ClientStatus.Idle,
            activeEffects = 0,
            powerLimit = null,
            firmwareVersion = "1.0",
            fps = 35,
            fadeTimeoutMillis = 0,
        )
        val map = toMap(LedClientEvent.LedClientCreated(id, client))
        map["type"] shouldBe "LedClientCreated"
        val data = map.data()
        data["uuid"] shouldBe id
        data["name"] shouldBe "Living Room"
        data["colorOrder"] shouldBe "GRB"
    }

    "LedStripCreated carries the strip object" {
        val id = uuid()
        val strip = GetLedStripResponse(
            clientUuid = "client-1",
            name = "Strip A",
            uuid = id,
            pin = "D18",
            length = 60,
            height = 1,
            brightness = 20,
            blendMode = BlendMode.Additive,
            inUse = false,
        )
        val map = toMap(LedStripEvent.LedStripCreated(id, strip))
        map["type"] shouldBe "LedStripCreated"
        val data = map.data()
        data["uuid"] shouldBe id
        data["pin"] shouldBe "D18"
        data.intField("length") shouldBe 60
    }

    "StripPoolCreated carries the pool object" {
        val id = uuid()
        val pool = GetStripPoolResponse(
            name = "Pool A",
            uuid = id,
            poolType = PoolType.Sync,
            blendMode = BlendMode.Layer,
            members = listOf(StripPoolMemberResponseModel("m1", "strip-1", false, 0, false)),
            inUse = false,
        )
        val map = toMap(StripPoolEvent.StripPoolCreated(id, pool))
        map["type"] shouldBe "StripPoolCreated"
        val data = map.data()
        data["uuid"] shouldBe id
        data["poolType"] shouldBe "Sync"
        (data["members"] as List<*>).size shouldBe 1
    }

    "PaletteCreated carries the palette object" {
        val id = uuid()
        val palette = GetPaletteResponse("Sunset", id, "Gradient", mapOf("colors" to listOf("ff0000")))
        val map = toMap(PaletteEvent.PaletteCreated(id, palette))
        map["type"] shouldBe "PaletteCreated"
        val data = map.data()
        data["uuid"] shouldBe id
        data["name"] shouldBe "Sunset"
    }

    "EffectSettingsCreated carries the settings object" {
        val id = uuid()
        val settings = GetEffectSettingsResponse(id, "Flame", "Hot", mapOf("cooling" to 11), true, true)
        val map = toMap(EffectSettingsEvent.EffectSettingsCreated(id, settings))
        map["type"] shouldBe "EffectSettingsCreated"
        val data = map.data()
        data["uuid"] shouldBe id
        data["isDefault"] shouldBe true
    }

    // endregion

    // region Updated events carry a delta of only the changed fields

    "LightEffectUpdated carries only changed fields" {
        val id = uuid()
        val map = toMap(LightEffectEvent.LightEffectUpdated(id, EffectDelta(name = "My Effect", layer = 2)))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LightEffectUpdated"
        val data = map.data()
        data.keys shouldBe setOf("name", "layer")
        data["name"] shouldBe "My Effect"
        data.intField("layer") shouldBe 2
    }

    "LedClientUpdated carries only changed fields" {
        val id = uuid()
        val map = toMap(LedClientEvent.LedClientUpdated(id, ClientDelta(name = "New Name", fps = 60)))
        map["type"] shouldBe "LedClientUpdated"
        val data = map.data()
        data.keys shouldBe setOf("name", "fps")
        data["name"] shouldBe "New Name"
        data.intField("fps") shouldBe 60
    }

    "LedStripUpdated carries only changed fields" {
        val id = uuid()
        val map = toMap(LedStripEvent.LedStripUpdated(id, StripDelta(brightness = 50)))
        map["type"] shouldBe "LedStripUpdated"
        val data = map.data()
        data.keys shouldBe setOf("brightness")
        data.intField("brightness") shouldBe 50
    }

    "StripPoolUpdated carries only changed fields" {
        val id = uuid()
        val map = toMap(StripPoolEvent.StripPoolUpdated(id, PoolDelta(name = "Renamed")))
        map["type"] shouldBe "StripPoolUpdated"
        map.data() shouldBe mapOf("name" to "Renamed")
    }

    "PaletteUpdated carries only changed fields" {
        val id = uuid()
        val map = toMap(PaletteEvent.PaletteUpdated(id, PaletteDelta(settings = mapOf("colors" to listOf("00ff00")))))
        map["type"] shouldBe "PaletteUpdated"
        val data = map.data()
        data shouldContainKey "settings"
        data shouldNotContainKey "name"
    }

    "EffectSettingsUpdated carries only changed fields" {
        val id = uuid()
        val map = toMap(EffectSettingsEvent.EffectSettingsUpdated(id, EffectSettingsDelta(skipFramesIfBlank = false)))
        map["type"] shouldBe "EffectSettingsUpdated"
        map.data() shouldBe mapOf("skipFramesIfBlank" to false)
    }

    // endregion

    // region Deleted events carry no payload

    "LightEffectDeleted has no data payload" {
        val id = uuid()
        val map = toMap(LightEffectEvent.LightEffectDeleted(id))
        map["uuid"] shouldBe id
        map["type"] shouldBe "LightEffectDeleted"
        map shouldNotContainKey "data"
    }

    "LedClientDeleted has no data payload" {
        val id = uuid()
        val map = toMap(LedClientEvent.LedClientDeleted(id))
        map["type"] shouldBe "LedClientDeleted"
        map shouldNotContainKey "data"
    }

    "LedStripDeleted has no data payload" {
        val id = uuid()
        val map = toMap(LedStripEvent.LedStripDeleted(id))
        map["type"] shouldBe "LedStripDeleted"
        map shouldNotContainKey "data"
    }

    "StripPoolDeleted has no data payload" {
        val id = uuid()
        val map = toMap(StripPoolEvent.StripPoolDeleted(id))
        map["type"] shouldBe "StripPoolDeleted"
        map shouldNotContainKey "data"
    }

    "PaletteDeleted has no data payload" {
        val id = uuid()
        val map = toMap(PaletteEvent.PaletteDeleted(id))
        map["type"] shouldBe "PaletteDeleted"
        map shouldNotContainKey "data"
    }

    "EffectSettingsDeleted has no data payload" {
        val id = uuid()
        val map = toMap(EffectSettingsEvent.EffectSettingsDeleted(id))
        map["type"] shouldBe "EffectSettingsDeleted"
        map shouldNotContainKey "data"
    }

    // endregion
})
