package io.cyborgsquirrel.lighting.effect_trigger.repository

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.led_strips.repository.PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.effect_trigger.LightEffectTriggerConstants
import io.cyborgsquirrel.lighting.effect_trigger.entity.LightEffectTriggerEntity
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effect_settings.entity.LightEffectSettingsEntity
import io.cyborgsquirrel.lighting.effect_settings.repository.LightEffectSettingsRepository
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.test_helpers.objectToMap
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.time.LocalTime
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LightEffectTriggerRepositoryTest(
    private val objectMapper: ObjectMapper,
    private val lightEffectRepository: LightEffectRepository,
    private val settingsRepository: LightEffectSettingsRepository,
    private val clientRepository: LedStripClientRepository,
    private val ledStripRepository: LedStripRepository,
    private val ledStripPoolRepository: LedStripPoolRepository,
    private val poolMemberLedStripRepository: PoolMemberLedStripRepository,
    private val lightEffectTriggerRepository: LightEffectTriggerRepository,
) : StringSpec({

    val nightriderLightEffectSettings = NightriderColorFillEffectSettings()
    val timeTriggerSettings =
        TimeTriggerSettings(
            LocalTime.of(19, 0), null, Duration.ofHours(4), maxActivations = null, triggerType = TriggerType.StartEffect
        )

    fun verifyLightEffectTriggerEntity(
        newEntity: LightEffectTriggerEntity,
        expectedEntity: LightEffectTriggerEntity,
    ) {
        newEntity.effect?.id shouldBe expectedEntity.effect?.id
        newEntity.uuid shouldBe expectedEntity.uuid
        newEntity.name shouldBe expectedEntity.name
        newEntity.settings shouldBe expectedEntity.settings
    }

    afterTest {
        lightEffectTriggerRepository.deleteAll()
        poolMemberLedStripRepository.deleteAll()
        lightEffectRepository.deleteAll()
        settingsRepository.deleteAll()
        ledStripPoolRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a light effect trigger entity" {
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
        val lightEffectSettingsJson = objectToMap(objectMapper, nightriderLightEffectSettings)
        val settingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = lightEffectSettingsJson,
                isDefault = false,
            )
        )
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                effectSettings = settingsEntity,
                name = "My nightrider effect",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
            )
        )
        val triggerSettingsJson = objectToMap(objectMapper, timeTriggerSettings)
        val trigger = lightEffectTriggerRepository.save(
            LightEffectTriggerEntity(
                effect = lightEffect,
                uuid = UUID.randomUUID().toString(),
                settings = triggerSettingsJson,
                type = LightEffectTriggerConstants.TIME_TRIGGER_NAME,
                name = "7pm trigger",
            )
        )

        val newEntities = lightEffectTriggerRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectTriggerEntity(newEntities.first(), trigger)
    }
})
