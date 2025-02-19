package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.client_config.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripGroupRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.*
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.SpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.SpectrumLightEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.model.color.RgbColor
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.json.tree.JsonNode
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
    private val objectMapper: ObjectMapper,
) : StringSpec({

    val settings = SpectrumLightEffectSettings(10, listOf(RgbColor.Red, RgbColor.Orange, RgbColor.Yellow))

    fun settingsObjectToMap(settings: Any): Map<String, Any> {
        val jsonNode = objectMapper.writeValueToTree(settings)
        return jsonNode.entries().associate { it.key to it.value.value }
    }

    afterTest {
        activeLightEffectRegistry.reset()
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
            objectMapper,
        )

        val client = clientRepository.save(
            LedStripClientEntity(name = "Living Room", address = "192.168.1.1", apiPort = 1111, wsPort = 2222)
        )
        val strip = ledStripRepository.save(
            LedStripEntity(client = client, uuid = UUID.randomUUID().toString(), name = "Strip A", length = 60)
        )
        val settingsJson = settingsObjectToMap(settings)
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
        activeEffectList.first().effect.settings shouldBe settings
        activeEffectList.first().strip.getUuid() shouldBe strip.uuid
        activeEffectList.first().strip.getName() shouldBe strip.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().uuid shouldBe lightEffect.uuid
    }

    "Init light effect one group - happy path" {
        val job = LightEffectInitJob(
            lightEffectRepository,
            groupMemberLedStripRepository,
            activeLightEffectRegistry,
            objectMapper,
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
        val settingsJson = settingsObjectToMap(settings)
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
        activeEffectList.first().effect.settings shouldBe settings
        activeEffectList.first().strip.getUuid() shouldBe group.uuid
        activeEffectList.first().strip.getName() shouldBe group.name
        activeEffectList.first().status shouldBe lightEffect.status
        activeEffectList.first().uuid shouldBe lightEffect.uuid
    }
})