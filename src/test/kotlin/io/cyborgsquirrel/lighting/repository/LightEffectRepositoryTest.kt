package io.cyborgsquirrel.lighting.repository

import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.entity.LightEffectLedStripAssociationEntity
import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.SunriseSunsetTriggerSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.micronaut.json.tree.JsonNode
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
    private val associationRepository: H2LightEffectLedStripAssociationRepository,
) : StringSpec({

    val settings =
        SunriseSunsetTriggerSettings(SunriseSunsetOption.Sunset, Duration.ofMinutes(15), null, TriggerType.StartEffect)
    var settingsJson: Map<String, Any>? = null
    lateinit var lightEffectEntity: LightEffectEntity
    lateinit var ledStripClientEntity: LedStripClientEntity
    lateinit var ledStripEntity: LedStripEntity
    lateinit var associationEntity: LightEffectLedStripAssociationEntity

    beforeTest {
        if (settingsJson == null) {
            val jsonNode = objectMapper.writeValueToTree(settings)
            settingsJson = jsonNode.entries().associate { it.key to it.value.value }
            lightEffectEntity =
                LightEffectEntity(settings = settingsJson, status = LightEffectStatus.Created, name = "Nightrider")
        }
    }

    afterTest {
        associationRepository.deleteAll()
        lightEffectRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a light effect entity" {
        val savedEntity = lightEffectRepository.save(lightEffectEntity)
        // Default id value is -1 because it is not valid in SQL
        // If this saved successfully we should have an auto-generated id greater than 0
        savedEntity.id shouldBeGreaterThan 0
    }

    "Query a light effect entity by name" {
        lightEffectRepository.save(lightEffectEntity)
        val retrievedEntityOptional = lightEffectRepository.findByName(lightEffectEntity.name!!)

        retrievedEntityOptional.isPresent shouldBe true

        val retrievedEntity = retrievedEntityOptional.get()
        retrievedEntity.name shouldBe retrievedEntity.name
        retrievedEntity.status shouldBe retrievedEntity.status

        val retrievedSettings =
            objectMapper.readValueFromTree(
                JsonNode.from(retrievedEntity.settings),
                SunriseSunsetTriggerSettings::class.java
            )

        retrievedSettings.sunriseSunsetOption shouldBe settings.sunriseSunsetOption
        retrievedSettings.activationDuration shouldBe settings.activationDuration
        retrievedSettings.triggerType shouldBe settings.triggerType
        retrievedSettings.maxActivations shouldBe settings.maxActivations
    }

    "Join with associations" {
        lightEffectEntity = lightEffectRepository.save(lightEffectEntity)
        ledStripClientEntity =
            LedStripClientEntity(name = "Living Room", address = "192.168.1.1", apiPort = 1111, wsPort = 2222)
        ledStripClientEntity = clientRepository.save(ledStripClientEntity)
        ledStripEntity = LedStripEntity(
            client = ledStripClientEntity,
            uuid = UUID.randomUUID().toString(),
            name = "Strip A",
            length = 60
        )
        ledStripEntity = ledStripRepository.save(ledStripEntity)
        associationEntity = LightEffectLedStripAssociationEntity(strip = ledStripEntity, effect = lightEffectEntity)
        associationEntity = associationRepository.save(associationEntity)

        val newLightEffectEntityOptional = lightEffectRepository.findByName(lightEffectEntity.name!!)

        newLightEffectEntityOptional.isPresent shouldBe true

        val newLightEffectEntity = newLightEffectEntityOptional.get()
        newLightEffectEntity.associations.size shouldBe 1
        newLightEffectEntity.associations.first().id shouldBe associationEntity.id
    }
})