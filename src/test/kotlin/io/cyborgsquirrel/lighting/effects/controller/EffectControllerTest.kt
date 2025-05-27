package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.api.EffectSetupApi
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterRepository
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.objectToMap
import io.cyborgsquirrel.test_helpers.saveLedStrips
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class EffectControllerTest(
    @Client private val apiClient: EffectSetupApi,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val triggerRepository: H2LightEffectRepository,
    private val filterRepository: H2LightEffectFilterRepository,
    private val objectMapper: ObjectMapper
) : StringSpec({

    afterEach {
        triggerRepository.deleteAll()
        filterRepository.deleteAll()
        effectRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Getting all effects" {
        val client = createLedStripClientEntity(clientRepository, "Porch lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrips(stripRepository, client, listOf("Strip A" to 200)).first()
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var effectEntity = LightEffectEntity(
            strip = strip,
            name = "Super cool effect",
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Created,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        val getAllEffectsHttpResponse = apiClient.getAllEffects()
        getAllEffectsHttpResponse.status shouldBe HttpStatus.OK

        val getAllEffectsResponse = getAllEffectsHttpResponse.body()
        getAllEffectsResponse.effects.size shouldBe 1
        val effectFromApi = getAllEffectsResponse.effects.first()

        effectFromApi.name shouldBe effectEntity.name
        effectFromApi.uuid shouldBe effectEntity.uuid
        effectFromApi.status shouldBe effectEntity.status
        effectFromApi.settings shouldBe effectEntity.settings
        effectFromApi.stripUuid shouldBe effectEntity.strip!!.uuid
    }

    "Getting effects for a strip" {
        val client = createLedStripClientEntity(clientRepository, "Living Room lights", "192.168.50.50", 50, 51)
        val strips = saveLedStrips(stripRepository, client, listOf("Strip A" to 200, "Strip B" to 100))
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var effectEntity = LightEffectEntity(
            strip = strips.last(),
            name = "Super cool effect",
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Created,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        var getAllEffectsHttpResponse = apiClient.getEffectsForStrip(strips.first().uuid, null)
        getAllEffectsHttpResponse.status shouldBe HttpStatus.OK

        var getAllEffectsResponse = getAllEffectsHttpResponse.body() as GetEffectsResponse
        getAllEffectsResponse.effects.isEmpty() shouldBe true

        getAllEffectsHttpResponse = apiClient.getEffectsForStrip(strips.last().uuid, null)
        getAllEffectsResponse = getAllEffectsHttpResponse.body() as GetEffectsResponse
        getAllEffectsResponse.effects.size shouldBe 1
        val effectFromApi = getAllEffectsResponse.effects.first()

        effectFromApi.name shouldBe effectEntity.name
        effectFromApi.uuid shouldBe effectEntity.uuid
        effectFromApi.status shouldBe effectEntity.status
        effectFromApi.settings shouldBe effectEntity.settings
        effectFromApi.stripUuid shouldBe effectEntity.strip?.uuid
    }

    "Create an effect" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrips(stripRepository, client, listOf("Strip A" to 200)).first()
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val request = CreateEffectRequest(
            strip.uuid!!,
            LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            "Rainbow Nightrider",
            defaultNrSettings
        )

        val createEffectHttpResponse = apiClient.createEffect(request)
        createEffectHttpResponse.status shouldBe HttpStatus.CREATED
        val effectUuid = createEffectHttpResponse.body() as String

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        val effectEntity = effectEntities.first()
        effectEntity.strip?.uuid shouldBe request.stripUuid
        effectEntity.name shouldBe request.name
        effectEntity.uuid shouldBe effectUuid
        effectEntity.settings shouldBe request.settings
    }

    "Updating an effect" {
        val client = createLedStripClientEntity(clientRepository, "Bedroom lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrips(stripRepository, client, listOf("Strip A" to 200)).first()
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val updatedNrSettings = objectToMap(
            objectMapper,
            NightriderEffectSettings.default().copy(listOf(RgbColor.Green, RgbColor.Cyan, RgbColor.Purple), true)
        )
        var effectEntity = LightEffectEntity(
            strip = strip,
            name = "Super cool effect",
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Created,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        val updateRequest = UpdateEffectRequest(
            name = "New name", settings = updatedNrSettings,
            stripUuid = null,
            status = LightEffectStatus.Activated,
        )
        val updateRequestHttpResponse = apiClient.updateEffect(effectEntity.uuid!!, updateRequest)
        updateRequestHttpResponse.status shouldBe HttpStatus.NO_CONTENT

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        effectEntities.first().strip?.uuid shouldBe strip.uuid
        effectEntities.first().name shouldBe updateRequest.name
        effectEntities.first().uuid shouldBe effectEntity.uuid
        effectEntities.first().settings shouldBe updateRequest.settings
        effectEntities.first().status shouldBe updateRequest.status
    }

    "Deleting an effect" {
        val client = createLedStripClientEntity(clientRepository, "Christmas Tree lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrips(stripRepository, client, listOf("Strip A" to 200)).first()
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var effectEntity = LightEffectEntity(
            strip = strip,
            name = "Super cool effect",
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Created,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        val createEffectHttpResponse = apiClient.deleteEffect(effectEntity.uuid!!)
        createEffectHttpResponse.status shouldBe HttpStatus.NO_CONTENT
        effectRepository.findAll().isEmpty() shouldBe true
    }
})