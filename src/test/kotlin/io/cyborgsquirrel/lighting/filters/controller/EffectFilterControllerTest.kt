package io.cyborgsquirrel.lighting.filters.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.api.EffectApi
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.lighting.filters.LightEffectFilterConstants
import io.cyborgsquirrel.lighting.filters.ReflectionFilter
import io.cyborgsquirrel.lighting.filters.api.EffectFilterApi
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterJunctionEntity
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterJunctionRepository
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterRepository
import io.cyborgsquirrel.lighting.filters.requests.CreateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.requests.UpdateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.responses.GetFilterResponse
import io.cyborgsquirrel.lighting.filters.responses.GetFiltersResponse
import io.cyborgsquirrel.lighting.filters.settings.IntensityFadeFilterSettings
import io.cyborgsquirrel.lighting.filters.settings.IntensityFilterSettings
import io.cyborgsquirrel.lighting.filters.settings.ReflectionFilterSettings
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.objectToMap
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.util.*

@MicronautTest
class EffectFilterControllerTest(
    @Client private val apiClient: EffectFilterApi,
    @Client private val effectApiClient: EffectApi,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val filterRepository: H2LightEffectFilterRepository,
    private val effectRegistry: ActiveLightEffectRegistry,
    private val junctionRepository: H2LightEffectFilterJunctionRepository,
    private val objectMapper: ObjectMapper
) : StringSpec({

    afterEach {
        filterRepository.deleteAll()
        effectRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Getting filter by uuid" {
        val defaultBrightnessSettings = objectToMap(objectMapper, IntensityFilterSettings(0.5f))
        val filterEntity = LightEffectFilterEntity(
            name = "Half brightness filter",
            uuid = UUID.randomUUID().toString(),
            settings = defaultBrightnessSettings,
            type = LightEffectFilterConstants.INTENSITY_FILTER_NAME
        )
        filterRepository.save(filterEntity)

        val getAllFilterHttpResponse = apiClient.getEffectFilter(filterEntity.uuid!!)
        getAllFilterHttpResponse.status shouldBe HttpStatus.OK

        val getAllFilterResponse = getAllFilterHttpResponse.body() as GetFilterResponse

        getAllFilterResponse.name shouldBe filterEntity.name
        getAllFilterResponse.type shouldBe LightEffectFilterConstants.INTENSITY_FILTER_NAME
        getAllFilterResponse.uuid shouldBe filterEntity.uuid
        getAllFilterResponse.settings shouldBe filterEntity.settings
    }

    "Getting filters for an effect" {
        val client = createLedStripClientEntity(clientRepository, "Living Room lights", "192.168.50.50", 50, 51)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D10.pinName, 50)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 100, PiClientPin.D21.pinName, 50)
        val strips = listOf(stripA, stripB)
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var effectEntity = LightEffectEntity(
            strip = strips.last(),
            name = "Super cool effect",
            type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Idle,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)
        var filterEntity = LightEffectFilterEntity(
            name = "My reverse filter",
            uuid = UUID.randomUUID().toString(),
            settings = mapOf(),
            type = LightEffectFilterConstants.REVERSE_FILTER_NAME
        )
        filterEntity = filterRepository.save(filterEntity)
        val junctionEntity = LightEffectFilterJunctionEntity(filter = filterEntity, effect = effectEntity)
        junctionRepository.save(junctionEntity)

        val getAllFiltersHttpResponse = apiClient.getFiltersForEffect(effectEntity.uuid!!)
        getAllFiltersHttpResponse.status shouldBe HttpStatus.OK

        val getAllFilterResponse = getAllFiltersHttpResponse.body() as GetFiltersResponse
        getAllFilterResponse.filters.size shouldBe 1
        val filterFromApi = getAllFilterResponse.filters.first()

        filterFromApi.name shouldBe filterEntity.name
        filterFromApi.type shouldBe LightEffectFilterConstants.REVERSE_FILTER_NAME
        filterFromApi.uuid shouldBe filterEntity.uuid
        filterFromApi.settings shouldBe filterEntity.settings
        filterFromApi.effectUuids shouldBe listOf(effectEntity.uuid)
    }

    "Create a filter" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 50)
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val createEffectHttpResponse = effectApiClient.createEffect(
            CreateEffectRequest(
                strip.uuid!!,
                null,
                LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                "Super cool effect",
                defaultNrSettings,
                null
            )
        )

        val effectUuid = (createEffectHttpResponse.body() as String)
        val intensityFadeFilterSettings = IntensityFadeFilterSettings(0.25f, 1.0f, Duration.ofSeconds(15))
        val settingsAsMap = objectToMap(objectMapper, intensityFadeFilterSettings)
        val request = CreateEffectFilterRequest(
            "My brightness fade filter",
            LightEffectFilterConstants.INTENSITY_FADE_FILTER_NAME,
            settingsAsMap
        )

        val createFilterHttpResponse = apiClient.createEffectFilter(request)
        createFilterHttpResponse.status shouldBe HttpStatus.CREATED
        val filterUuid = createFilterHttpResponse.body() as String

        val filterEntityOptional = filterRepository.findByUuid(filterUuid)
        filterEntityOptional.isPresent shouldBe true
        val filterEntity = filterEntityOptional.get()

        filterEntity.name shouldBe request.name
        filterEntity.type shouldBe request.filterType
        filterEntity.settings shouldBe request.settings

        val activeEffectOptional = effectRegistry.getEffectWithUuid(effectUuid)
        activeEffectOptional.isPresent shouldBe true
        val activeEffect = activeEffectOptional.get()

        // Filter was created but not assigned to an effect
        activeEffect.filters.isEmpty() shouldBe true
    }

    "Updating a filter" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 50)
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val createEffectHttpResponse = effectApiClient.createEffect(
            CreateEffectRequest(
                strip.uuid!!,
                null,
                LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                "Super cool effect",
                defaultNrSettings,
                null
            )
        )
        val effectUuid = createEffectHttpResponse.body() as String

        val reflectionFilterSettings = ReflectionFilterSettings(ReflectionType.CopyOverCenter)
        val settingsAsMap = objectToMap(objectMapper, reflectionFilterSettings)
        val createRequest = CreateEffectFilterRequest(
            "My brightness fade filter",
            LightEffectFilterConstants.REFLECTION_FILTER_NAME,
            settingsAsMap
        )
        val createFilterHttpResponse = apiClient.createEffectFilter(createRequest)
        val filterUuid = createFilterHttpResponse.body() as String

        val updatedSettings = reflectionFilterSettings.copy(ReflectionType.LowToHigh)
        val updatedSettingsMap = objectToMap(objectMapper, updatedSettings)
        val updateRequest = UpdateEffectFilterRequest(
            name = "My reflection filter UPDATED",
            effectUuids = listOf(effectUuid),
            settings = updatedSettingsMap
        )

        val updateFilterHttpResponse = apiClient.updateEffectFilter(filterUuid, updateRequest)
        updateFilterHttpResponse.status shouldBe HttpStatus.NO_CONTENT

        val filterEntityOptional = filterRepository.findByUuid(filterUuid)
        filterEntityOptional.isPresent shouldBe true
        val filterEntity = filterEntityOptional.get()

        val junctionEntities = junctionRepository.findByFilter(filterEntity)
        junctionEntities.size shouldBe 1
        val junctionEntity = junctionEntities.first()

        junctionEntity.effect?.uuid shouldBe effectUuid
        filterEntity.name shouldBe updateRequest.name
        filterEntity.settings shouldBe updateRequest.settings
        filterEntity.type shouldBe createRequest.filterType

        val activeEffectOptional = effectRegistry.getEffectWithUuid(effectUuid)
        activeEffectOptional.isPresent shouldBe true
        val activeEffect = activeEffectOptional.get()

        activeEffect.filters.size shouldBe 1
        val activeFilter = activeEffect.filters.first()

        (activeFilter as ReflectionFilter).uuid shouldBe filterUuid
        activeFilter.settings shouldBe updatedSettings
    }

    "Delete a filter" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 50)
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val createEffectHttpResponse = effectApiClient.createEffect(
            CreateEffectRequest(
                strip.uuid!!,
                null,
                LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                "Super cool effect",
                defaultNrSettings,
                null
            )
        )
        val effectUuid = createEffectHttpResponse.body() as String

        val reflectionFilterSettings = ReflectionFilterSettings(ReflectionType.CopyOverCenter)
        val settingsAsMap = objectToMap(objectMapper, reflectionFilterSettings)
        val createRequest = CreateEffectFilterRequest(
            "My brightness fade filter",
            LightEffectFilterConstants.REFLECTION_FILTER_NAME,
            settingsAsMap
        )
        val createFilterHttpResponse = apiClient.createEffectFilter(createRequest)
        val filterUuid = createFilterHttpResponse.body() as String

        val deleteResponse = apiClient.deleteEffectFilter(filterUuid)
        val filterOptional = filterRepository.findByUuid(effectUuid)

        filterOptional.isEmpty shouldBe true
        deleteResponse.status shouldBe HttpStatus.NO_CONTENT
    }
})