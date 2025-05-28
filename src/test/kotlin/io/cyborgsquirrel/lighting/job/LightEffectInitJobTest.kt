package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.led_strips.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripGroupRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.led_strips.entity.GroupMemberLedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripGroupEntity
import io.cyborgsquirrel.lighting.effect_trigger.LightEffectTriggerConstants
import io.cyborgsquirrel.lighting.effect_trigger.TriggerManager
import io.cyborgsquirrel.lighting.effect_trigger.entity.LightEffectTriggerEntity
import io.cyborgsquirrel.lighting.effect_trigger.repository.H2LightEffectTriggerRepository
import io.cyborgsquirrel.lighting.effect_trigger.settings.EffectIterationTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.EffectIterationTrigger
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.SpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.CreateLightingHelper
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.BrightnessFadeFilter
import io.cyborgsquirrel.lighting.filters.LightEffectFilterConstants
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterRepository
import io.cyborgsquirrel.lighting.filters.settings.BrightnessFadeFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.test_helpers.objectToMap
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LightEffectInitJobTest(
    private val clientRepository: H2LedStripClientRepository,
    private val lightEffectRepository: H2LightEffectRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val triggerRepository: H2LightEffectTriggerRepository,
    private val filterRepository: H2LightEffectFilterRepository,
    private val objectMapper: ObjectMapper,
    private val triggerManager: TriggerManager,
    private val effectFactory: CreateLightingHelper,
) : StringSpec({

    val lightEffectSettings =
        SpectrumEffectSettings.default(60).copy(10, listOf(RgbColor.Red, RgbColor.Orange, RgbColor.Yellow))
    val iterationTriggerSettings = EffectIterationTriggerSettings(25)
    val fadeFilterSettings = BrightnessFadeFilterSettings(0.0f, 1.0f, Duration.ofSeconds(20))

    afterTest {
        activeLightEffectRegistry.reset()
        triggerRepository.deleteAll()
        filterRepository.deleteAll()
        lightEffectRepository.deleteAll()
        groupMemberLedStripRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Init light effect one strip - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            activeLightEffectRegistry,
            triggerManager,
            effectFactory,
        )

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
        val settingsJson = objectToMap(objectMapper, lightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = settingsJson,
                type = LightEffectConstants.SPECTRUM_NAME,
                name = "Happy path effect",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
            )
        )

        job.run()

        val activeEffectList = activeLightEffectRegistry.getAllEffects()

        activeEffectList.size shouldBe 1
        activeEffectList.first().effect::class shouldBe SpectrumLightEffect::class
        activeEffectList.first().effect.getSettings()::class shouldBe SpectrumEffectSettings::class
        activeEffectList.first().strip.getUuid() shouldBe strip.uuid
        activeEffectList.first().strip.getName() shouldBe strip.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().effectUuid shouldBe lightEffect.uuid
        activeEffectList.first().strip.getBlendMode() shouldBe strip.blendMode
    }

    "Init light effect with trigger - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            activeLightEffectRegistry,
            triggerManager,
            effectFactory,
        )

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
        val lightEffectSettingsJson = objectToMap(objectMapper, lightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = lightEffectSettingsJson,
                type = LightEffectConstants.SPECTRUM_NAME,
                name = "A light effect",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
            )
        )
        val iterationTriggerSettingsJson = objectToMap(objectMapper, iterationTriggerSettings)
        val trigger = triggerRepository.save(
            LightEffectTriggerEntity(
                effect = lightEffect,
                uuid = UUID.randomUUID().toString(),
                name = LightEffectTriggerConstants.ITERATION_TRIGGER_NAME,
                settings = iterationTriggerSettingsJson,
            )
        )

        job.run()

        val activeEffectList = activeLightEffectRegistry.getAllEffects()

        activeEffectList.size shouldBe 1
        activeEffectList.first().effect::class shouldBe SpectrumLightEffect::class
        activeEffectList.first().effect.getSettings()::class shouldBe SpectrumEffectSettings::class
        activeEffectList.first().strip.getUuid() shouldBe strip.uuid
        activeEffectList.first().strip.getName() shouldBe strip.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().effectUuid shouldBe lightEffect.uuid

        val triggers = triggerManager.getTriggers()

        triggers.size shouldBe 1
        triggers.first()::class shouldBe EffectIterationTrigger::class
        triggers.first().uuid shouldBe trigger.uuid
        triggers.first().settings::class shouldBe iterationTriggerSettings::class
        triggers.first().settings.triggerType shouldBe iterationTriggerSettings.triggerType
        triggers.first().settings.activationDuration shouldBe iterationTriggerSettings.activationDuration
        triggers.first().settings.maxActivations shouldBe iterationTriggerSettings.maxActivations
    }

    "Init light effect with filter - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            activeLightEffectRegistry,
            triggerManager,
            effectFactory,
        )

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
        val lightEffectSettingsJson = objectToMap(objectMapper, lightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = lightEffectSettingsJson,
                type = LightEffectConstants.SPECTRUM_NAME,
                name = "Effect A",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
            )
        )
        val fadeTriggerSettingsJson = objectToMap(objectMapper, fadeFilterSettings)
        val filter = filterRepository.save(
            LightEffectFilterEntity(
                effect = lightEffect,
                uuid = UUID.randomUUID().toString(),
                name = LightEffectFilterConstants.BRIGHTNESS_FADE_FILTER_NAME,
                settings = fadeTriggerSettingsJson,
            )
        )

        job.run()

        val activeEffectList = activeLightEffectRegistry.getAllEffects()

        activeEffectList.size shouldBe 1
        activeEffectList.first().effect::class shouldBe SpectrumLightEffect::class
        activeEffectList.first().effect.getSettings()::class shouldBe SpectrumEffectSettings::class
        activeEffectList.first().strip.getUuid() shouldBe strip.uuid
        activeEffectList.first().strip.getName() shouldBe strip.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().effectUuid shouldBe lightEffect.uuid

        val filters = activeEffectList.first().filters

        filters.size shouldBe 1
        filters.first()::class shouldBe BrightnessFadeFilter::class
        filters.first().uuid shouldBe filter.uuid
        (filters.first() as BrightnessFadeFilter).settings::class shouldBe fadeFilterSettings::class
        (filters.first() as BrightnessFadeFilter).settings.startingBrightness shouldBe fadeFilterSettings.startingBrightness
        (filters.first() as BrightnessFadeFilter).settings.endingBrightness shouldBe fadeFilterSettings.endingBrightness
        (filters.first() as BrightnessFadeFilter).settings.fadeDuration shouldBe fadeFilterSettings.fadeDuration
    }

    "Init light effect one group - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            activeLightEffectRegistry,
            triggerManager,
            effectFactory,
        )

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
        val group = ledStripGroupRepository.save(
            LedStripGroupEntity(uuid = UUID.randomUUID().toString(), name = "Living Room Group")
        )
        val groupMember = groupMemberLedStripRepository.save(
            GroupMemberLedStripEntity(strip = strip, group = group, groupIndex = 0, inverted = false)
        )
        val settingsJson = objectToMap(objectMapper, lightEffectSettings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = settingsJson,
                type = LightEffectConstants.SPECTRUM_NAME,
                name = "Effect C",
                group = group,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
            )
        )

        job.run()

        val activeEffectList = activeLightEffectRegistry.getAllEffects()

        activeEffectList.size shouldBe 1
        activeEffectList.first().effect::class shouldBe SpectrumLightEffect::class
        activeEffectList.first().effect.getSettings()::class shouldBe SpectrumEffectSettings::class
        activeEffectList.first().strip.getUuid() shouldBe group.uuid
        activeEffectList.first().strip.getName() shouldBe group.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().effectUuid shouldBe lightEffect.uuid
    }
})