package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.client_config.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripGroupRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.*
import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.SunriseSunsetTriggerSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LightEffectRepositoryTest(
    private val objectMapper: ObjectMapper,
    private val lightEffectRepository: H2LightEffectRepository,
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
) : StringSpec({

    val settings =
        SunriseSunsetTriggerSettings(SunriseSunsetOption.Sunset, Duration.ofMinutes(15), null, TriggerType.StartEffect)

    fun settingsObjectToMap(settings: SunriseSunsetTriggerSettings): Map<String, Any> {
        val jsonNode = objectMapper.writeValueToTree(settings)
        return jsonNode.entries().associate { it.key to it.value.value }
    }

    fun verifyLightEffectEntity(
        newEntity: LightEffectEntity,
        expectedEntity: LightEffectEntity,
    ) {
        newEntity.strip?.id shouldBe expectedEntity.strip?.id
        newEntity.group?.id shouldBe expectedEntity.group?.id
        newEntity.uuid shouldBe expectedEntity.uuid
        newEntity.name shouldBe expectedEntity.name
        newEntity.settings shouldBe expectedEntity.settings
    }

    afterTest {
        groupMemberLedStripRepository.deleteAll()
        lightEffectRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a light effect association entity" {
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
                name = "Nightrider",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Created,
            )
        )

        val newEntities = lightEffectRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectEntity(newEntities.first(), lightEffect)
    }

    "Create a light effect association entity for a group" {
        val client = clientRepository.save(
            LedStripClientEntity(name = "Living Room", address = "192.168.1.1", apiPort = 1111, wsPort = 2222)
        )
        val strip = ledStripRepository.save(
            LedStripEntity(client = client, uuid = UUID.randomUUID().toString(), name = "Strip A", length = 60)
        )
        val group = ledStripGroupRepository.save(
            LedStripGroupEntity(
                name = "Living Room",
                uuid = UUID.randomUUID().toString()
            )
        )
        val groupMember = groupMemberLedStripRepository.save(
            GroupMemberLedStripEntity(
                group = group,
                strip = strip,
                inverted = false,
                groupIndex = 1
            )
        )

        val settingsJson = settingsObjectToMap(settings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = settingsJson,
                name = "Nightrider",
                group = group,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Created,
            )
        )

        val newEntities = lightEffectRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectEntity(newEntities.first(), lightEffect)
        newEntities.first().group?.members?.first()?.id shouldBe groupMember.id
    }
})