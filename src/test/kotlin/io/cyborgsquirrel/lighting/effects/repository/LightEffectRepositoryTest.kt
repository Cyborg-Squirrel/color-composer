package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.repository.LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.led_strips.repository.PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.effect_settings.entity.LightEffectSettingsEntity
import io.cyborgsquirrel.lighting.effect_settings.repository.LightEffectSettingsRepository
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.test_helpers.normalizeNumberTypes
import io.cyborgsquirrel.test_helpers.objectToMap
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LightEffectRepositoryTest(
    private val objectMapper: ObjectMapper,
    private val lightEffectRepository: LightEffectRepository,
    private val settingsRepository: LightEffectSettingsRepository,
    private val clientRepository: LedStripClientRepository,
    private val ledStripRepository: LedStripRepository,
    private val ledStripPoolRepository: LedStripPoolRepository,
    private val poolMemberLedStripRepository: PoolMemberLedStripRepository,
) : StringSpec({

    val settings = NightriderColorFillEffectSettings()

    fun verifyLightEffectEntity(
        newEntity: LightEffectEntity,
        expectedEntity: LightEffectEntity,
    ) {
        newEntity.strip?.id shouldBe expectedEntity.strip?.id
        newEntity.pool?.id shouldBe expectedEntity.pool?.id
        newEntity.uuid shouldBe expectedEntity.uuid
        newEntity.name shouldBe expectedEntity.name
        val actualSettings = newEntity.effectSettings.shouldNotBeNull()
        val expectedSettings = expectedEntity.effectSettings.shouldNotBeNull()
        actualSettings.settings.map { normalizeNumberTypes(it.value) } shouldBe
            expectedSettings.settings.map { normalizeNumberTypes(it.value) }
    }

    afterTest {
        poolMemberLedStripRepository.deleteAll()
        lightEffectRepository.deleteAll()
        settingsRepository.deleteAll()
        ledStripPoolRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a light effect entity" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Living Room",
                address = "192.168.1.1",
                clientType = ClientType.Pi,
                colorOrder = ColorOrder.RGB,
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222,
                firmwareVersion = "--",
                fps = 35,
                fadeTimeoutMillis = 0,
            )
        )
        val strip = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Strip A",
                pin = PiClientPin.D18.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val settingsJson = objectToMap(objectMapper, settings)
        val settingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = settingsJson,
                isDefault = false,
            )
        )
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                effectSettings = settingsEntity,
                name = "Super cool effect",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
            )
        )

        val newEntities = lightEffectRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectEntity(newEntities.first(), lightEffect)
    }

    "Create a light effect entity for a pool" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Living Room",
                address = "192.168.1.1",
                clientType = ClientType.Pi,
                colorOrder = ColorOrder.RGB,
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222,
                firmwareVersion = "--",
                fps = 35,
                fadeTimeoutMillis = 0,
            )
        )
        val strip = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Strip A",
                pin = PiClientPin.D10.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val pool = ledStripPoolRepository.save(
            LedStripPoolEntity(
                name = "Living Room",
                uuid = UUID.randomUUID().toString(),
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )
        val poolMember = poolMemberLedStripRepository.save(
            PoolMemberLedStripEntity(
                pool = pool,
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                inverted = false,
                poolIndex = 1
            )
        )

        val settingsJson = objectToMap(objectMapper, settings)
        val settingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = settingsJson,
                isDefault = false,
            )
        )
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                effectSettings = settingsEntity,
                name = "My effect",
                pool = pool,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
            )
        )

        val newEntities = lightEffectRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectEntity(newEntities.first(), lightEffect)
        newEntities.first().pool?.members?.first()?.id shouldBe poolMember.id
    }

    "layer and skipFramesIfBlank round-trip through persistence" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Roundtrip client",
                address = "192.168.1.2",
                clientType = ClientType.Pi,
                colorOrder = ColorOrder.RGB,
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222,
                firmwareVersion = "--",
                fps = 35,
                fadeTimeoutMillis = 0,
            )
        )
        val strip = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Roundtrip strip",
                pin = PiClientPin.D18.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val settingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Skip-blank-off settings",
                settings = objectToMap(objectMapper, settings),
                isDefault = false,
                skipFramesIfBlank = false,
            )
        )
        lightEffectRepository.save(
            LightEffectEntity(
                effectSettings = settingsEntity,
                name = "Layer 7 effect",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
                layer = 7,
            )
        )

        val fetched = lightEffectRepository.queryAll().single()
        fetched.layer shouldBe 7
        val fetchedSettings = fetched.effectSettings.shouldNotBeNull()
        fetchedSettings.skipFramesIfBlank shouldBe false
    }

    "Duplicate layer on the same strip is rejected by the unique constraint" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Dup-layer client",
                address = "192.168.1.3",
                clientType = ClientType.Pi,
                colorOrder = ColorOrder.RGB,
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222,
                firmwareVersion = "--",
                fps = 35,
                fadeTimeoutMillis = 0,
            )
        )
        val strip = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Dup-layer strip",
                pin = PiClientPin.D18.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val settingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Settings",
                settings = objectToMap(objectMapper, settings),
                isDefault = false,
            )
        )
        lightEffectRepository.save(
            LightEffectEntity(
                effectSettings = settingsEntity,
                name = "First on layer 0",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
                layer = 0,
            )
        )

        shouldThrow<DataAccessException> {
            lightEffectRepository.save(
                LightEffectEntity(
                    effectSettings = settingsEntity,
                    name = "Second on layer 0",
                    strip = strip,
                    uuid = UUID.randomUUID().toString(),
                    status = LightEffectStatus.Inactive,
                    layer = 0,
                )
            )
        }
    }

    "Same layer is allowed on different strips" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Cross-strip client",
                address = "192.168.1.4",
                clientType = ClientType.Pi,
                colorOrder = ColorOrder.RGB,
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222,
                firmwareVersion = "--",
                fps = 35,
                fadeTimeoutMillis = 0,
            )
        )
        val stripA = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Strip A",
                pin = PiClientPin.D18.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val stripB = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Strip B",
                pin = PiClientPin.D10.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val settingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Settings",
                settings = objectToMap(objectMapper, settings),
                isDefault = false,
            )
        )
        lightEffectRepository.save(
            LightEffectEntity(
                effectSettings = settingsEntity,
                name = "A layer 0",
                strip = stripA,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
                layer = 0,
            )
        )
        lightEffectRepository.save(
            LightEffectEntity(
                effectSettings = settingsEntity,
                name = "B layer 0",
                strip = stripB,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
                layer = 0,
            )
        )

        lightEffectRepository.queryAll().size shouldBe 2
    }
})
