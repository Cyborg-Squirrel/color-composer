package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.client_config.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripGroupRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.*
import io.cyborgsquirrel.lighting.effect_trigger.LightEffectTriggerConstants
import io.cyborgsquirrel.lighting.effect_trigger.TriggerManager
import io.cyborgsquirrel.lighting.effect_trigger.repository.H2LightEffectTriggerRepository
import io.cyborgsquirrel.lighting.effect_trigger.settings.EffectIterationTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.EffectIterationTrigger
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.SpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.SpectrumLightEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LightEffectInitJobTest(
    private val clientRepository: H2LedStripClientRepository,
    private val lightEffectRepository: H2LightEffectRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val triggerRepository: H2LightEffectTriggerRepository,
    private val objectMapper: ObjectMapper,
    private val triggerManager: TriggerManager,
    private val timeHelper: TimeHelper,
) : StringSpec({

    val lightEffectSettings =
        SpectrumLightEffectSettings.default(60).copy(10, listOf(RgbColor.Red, RgbColor.Orange, RgbColor.Yellow))
    val iterationTriggerSettings = EffectIterationTriggerSettings(25)

    fun settingsObjectToMap(settings: Any): Map<String, Any> {
        val jsonNode = objectMapper.writeValueToTree(settings)
        return jsonNode.entries().associate { it.key to it.value.value }
    }

    afterTest {
        activeLightEffectRegistry.reset()
        triggerRepository.deleteAll()
        lightEffectRepository.deleteAll()
        groupMemberLedStripRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Init light effect one strip - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            groupMemberLedStripRepository,
            activeLightEffectRegistry,
            sunriseSunsetTimeRepository,
            locationConfigRepository,
            triggerManager,
            objectMapper,
            timeHelper,
        )

        val client = clientRepository.save(
            LedStripClientEntity(name = "Living Room", address = "192.168.1.1", apiPort = 1111, wsPort = 2222)
        )
        val strip = ledStripRepository.save(
            LedStripEntity(client = client, uuid = UUID.randomUUID().toString(), name = "Strip A", length = 60)
        )
        val settingsJson = settingsObjectToMap(lightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = settingsJson,
                name = LightEffectConstants.SPECTRUM_NAME,
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Created,
            )
        )

        job.run()

        val activeEffectList = activeLightEffectRegistry.findAllEffects()

        activeEffectList.size shouldBe 1
        activeEffectList.first().effect::class shouldBe SpectrumLightEffect::class
        activeEffectList.first().effect.getSettings()::class shouldBe SpectrumLightEffectSettings::class
        activeEffectList.first().strip.getUuid() shouldBe strip.uuid
        activeEffectList.first().strip.getName() shouldBe strip.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().uuid shouldBe lightEffect.uuid
    }

    "Init light effect with trigger - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            groupMemberLedStripRepository,
            activeLightEffectRegistry,
            sunriseSunsetTimeRepository,
            locationConfigRepository,
            triggerManager,
            objectMapper,
            timeHelper,
        )

        val client = clientRepository.save(
            LedStripClientEntity(name = "Living Room", address = "192.168.1.1", apiPort = 1111, wsPort = 2222)
        )
        val strip = ledStripRepository.save(
            LedStripEntity(client = client, uuid = UUID.randomUUID().toString(), name = "Strip A", length = 60)
        )
        val lightEffectSettingsJson = settingsObjectToMap(lightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = lightEffectSettingsJson,
                name = LightEffectConstants.SPECTRUM_NAME,
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Created,
            )
        )
        val iterationTriggerSettingsJson = settingsObjectToMap(iterationTriggerSettings)
        val trigger = triggerRepository.save(
            LightEffectTriggerEntity(
                effect = lightEffect,
                uuid = UUID.randomUUID().toString(),
                name = LightEffectTriggerConstants.ITERATION_TRIGGER_NAME,
                settings = iterationTriggerSettingsJson,
            )
        )

        job.run()

        val activeEffectList = activeLightEffectRegistry.findAllEffects()

        activeEffectList.size shouldBe 1
        activeEffectList.first().effect::class shouldBe SpectrumLightEffect::class
        activeEffectList.first().effect.getSettings()::class shouldBe SpectrumLightEffectSettings::class
        activeEffectList.first().strip.getUuid() shouldBe strip.uuid
        activeEffectList.first().strip.getName() shouldBe strip.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().uuid shouldBe lightEffect.uuid

        val triggers = triggerManager.getTriggers()

        triggers.size shouldBe 1
        triggers.first()::class shouldBe EffectIterationTrigger::class
        triggers.first().uuid shouldBe trigger.uuid
        triggers.first().settings::class shouldBe iterationTriggerSettings::class
        triggers.first().settings.triggerType shouldBe iterationTriggerSettings.triggerType
        triggers.first().settings.activationDuration shouldBe iterationTriggerSettings.activationDuration
        triggers.first().settings.maxActivations shouldBe iterationTriggerSettings.maxActivations
    }

    "Init light effect one group - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            groupMemberLedStripRepository,
            activeLightEffectRegistry,
            sunriseSunsetTimeRepository,
            locationConfigRepository,
            triggerManager,
            objectMapper,
            timeHelper,
        )

        val client = clientRepository.save(
            LedStripClientEntity(name = "Living Room", address = "192.168.1.1", apiPort = 1111, wsPort = 2222)
        )
        val strip = ledStripRepository.save(
            LedStripEntity(client = client, uuid = UUID.randomUUID().toString(), name = "Strip A", length = 60)
        )
        val group = ledStripGroupRepository.save(
            LedStripGroupEntity(uuid = UUID.randomUUID().toString(), name = "Living Room Group")
        )
        val groupMember = groupMemberLedStripRepository.save(
            GroupMemberLedStripEntity(strip = strip, group = group, groupIndex = 0, inverted = false)
        )
        val settingsJson = settingsObjectToMap(lightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = settingsJson,
                name = LightEffectConstants.SPECTRUM_NAME,
                group = group,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Created,
            )
        )

        job.run()

        val activeEffectList = activeLightEffectRegistry.findAllEffects()

        activeEffectList.size shouldBe 1
        activeEffectList.first().effect::class shouldBe SpectrumLightEffect::class
        activeEffectList.first().effect.getSettings()::class shouldBe SpectrumLightEffectSettings::class
        activeEffectList.first().strip.getUuid() shouldBe group.uuid
        activeEffectList.first().strip.getName() shouldBe group.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().uuid shouldBe lightEffect.uuid
    }
})