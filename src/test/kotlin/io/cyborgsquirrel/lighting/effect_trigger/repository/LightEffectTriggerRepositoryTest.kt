package io.cyborgsquirrel.lighting.effect_trigger.repository

import io.cyborgsquirrel.client_config.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripGroupRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.entity.LightEffectTriggerEntity
import io.cyborgsquirrel.lighting.effect_trigger.LightEffectTriggerConstants
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.model.color.RgbColor
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
    private val lightEffectRepository: H2LightEffectRepository,
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
    private val lightEffectTriggerRepository: H2LightEffectTriggerRepository,
) : StringSpec({

    val nightriderLightEffectSettings =
        NightriderEffectSettings.default().copy(colorList = listOf(RgbColor.Red, RgbColor.Orange, RgbColor.Yellow))
    val timeTriggerSettings =
        TimeTriggerSettings(
            LocalTime.of(19, 0), Duration.ofHours(4), maxActivations = null, triggerType = TriggerType.StartEffect
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
        groupMemberLedStripRepository.deleteAll()
        lightEffectRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a light effect trigger entity" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Living Room",
                address = "192.168.1.1",
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222
            )
        )
        val strip = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Strip A",
                length = 60,
                blendMode = BlendMode.Average
            )
        )
        val lightEffectSettingsJson = objectToMap(objectMapper, nightriderLightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = lightEffectSettingsJson,
                name = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Created,
            )
        )
        val triggerSettingsJson = objectToMap(objectMapper, timeTriggerSettings)
        val trigger = lightEffectTriggerRepository.save(
            LightEffectTriggerEntity(
                effect = lightEffect,
                uuid = UUID.randomUUID().toString(),
                settings = triggerSettingsJson,
                name = LightEffectTriggerConstants.TIME_TRIGGER_NAME,
            )
        )

        val newEntities = lightEffectTriggerRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectTriggerEntity(newEntities.first(), trigger)
    }
})