package io.cyborgsquirrel.lighting.filters.service

import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.lighting.effect_settings.repository.LightEffectSettingsRepository
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.filters.LightEffectFilterConstants
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterJunctionEntity
import io.cyborgsquirrel.lighting.filters.repository.LightEffectFilterJunctionRepository
import io.cyborgsquirrel.lighting.filters.repository.LightEffectFilterRepository
import io.cyborgsquirrel.lighting.filters.requests.CreateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.requests.UpdateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.settings.IntensityFilterSettings
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.objectToMap
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.test_helpers.saveLightEffect
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class EffectFilterApiServiceTest(
    private val filterApiService: EffectFilterApiService,
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val effectRepository: LightEffectRepository,
    private val settingsRepository: LightEffectSettingsRepository,
    private val filterRepository: LightEffectFilterRepository,
    private val junctionRepository: LightEffectFilterJunctionRepository,
    private val objectMapper: ObjectMapper,
) : StringSpec({

    afterEach {
        junctionRepository.deleteAll()
        effectRepository.deleteAll()
        filterRepository.deleteAll()
        settingsRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    fun saveFilter(name: String) = filterRepository.save(
        LightEffectFilterEntity(
            name = name,
            uuid = UUID.randomUUID().toString(),
            settings = objectToMap(objectMapper, IntensityFilterSettings(0.5f)),
            type = LightEffectFilterConstants.INTENSITY_FILTER_NAME,
        )
    )

    "getAllFilters returns an empty response when no filters exist" {
        val response = filterApiService.getAllFilters()
        response.filters.isEmpty() shouldBe true
    }

    "getAllFilters returns every filter with its assigned effect uuids resolved" {
        val client = createLedStripClientEntity(clientRepository, "Living room", "192.168.1.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D10.pinName, 100)
        val effect = saveLightEffect(effectRepository, objectMapper, settingsRepository, strip)

        // One filter assigned to an effect, one with no assignments
        val assignedFilter = saveFilter("Assigned filter")
        val unassignedFilter = saveFilter("Unassigned filter")
        junctionRepository.save(LightEffectFilterJunctionEntity(filter = assignedFilter, effect = effect))

        val response = filterApiService.getAllFilters()

        response.filters.size shouldBe 2
        val byUuid = response.filters.associateBy { it.uuid }
        byUuid[assignedFilter.uuid]!!.effectUuids shouldBe listOf(effect.uuid)
        byUuid[unassignedFilter.uuid]!!.effectUuids.isEmpty() shouldBe true
    }

    "getFilter returns the filter with its assigned effect uuids" {
        val client = createLedStripClientEntity(clientRepository, "Bedroom", "192.168.1.51", 52, 53)
        val strip = saveLedStrip(stripRepository, client, "Strip B", 60, PiClientPin.D10.pinName, 100)
        val effect = saveLightEffect(effectRepository, objectMapper, settingsRepository, strip)
        val filter = saveFilter("My filter")
        junctionRepository.save(LightEffectFilterJunctionEntity(filter = filter, effect = effect))

        val response = filterApiService.getFilter(filter.uuid)

        response.uuid shouldBe filter.uuid
        response.name shouldBe filter.name
        response.type shouldBe LightEffectFilterConstants.INTENSITY_FILTER_NAME
        response.settings shouldBe filter.settings
        response.effectUuids shouldBe listOf(effect.uuid)
    }

    "getFilter throws ResourceNotFoundException for a non-existent filter" {
        shouldThrow<ResourceNotFoundException> {
            filterApiService.getFilter("non-existent-uuid")
        }
    }

    "getFiltersForEffect throws ClientRequestException for a non-existent effect" {
        shouldThrow<ClientRequestException> {
            filterApiService.getFiltersForEffect("non-existent-effect")
        }
    }

    "createFilter persists the filter and returns its uuid" {
        val request = CreateEffectFilterRequest(
            "Half brightness",
            LightEffectFilterConstants.INTENSITY_FILTER_NAME,
            objectToMap(objectMapper, IntensityFilterSettings(0.5f)),
        )

        val uuid = filterApiService.createFilter(request)

        val saved = filterRepository.findByUuid(uuid)
        saved.isPresent shouldBe true
        saved.get().name shouldBe request.name
        saved.get().type shouldBe request.filterType
        saved.get().settings shouldBe request.settings
    }

    "updateFilter throws ResourceNotFoundException for a non-existent filter" {
        val request = UpdateEffectFilterRequest(name = "Updated", effectUuids = emptyList(), settings = null)
        shouldThrow<ResourceNotFoundException> {
            filterApiService.updateFilter("non-existent-uuid", request)
        }
    }

    "updateFilter throws ClientRequestException when assigning a non-existent effect" {
        val filter = saveFilter("Filter to update")
        val request = UpdateEffectFilterRequest(
            name = "Updated",
            effectUuids = listOf(UUID.randomUUID().toString()),
            settings = null,
        )

        shouldThrow<ClientRequestException> {
            filterApiService.updateFilter(filter.uuid, request)
        }
    }

    "deleteFilter throws ResourceNotFoundException for a non-existent filter" {
        shouldThrow<ResourceNotFoundException> {
            filterApiService.deleteFilter("non-existent-uuid")
        }
    }

    "deleteFilter removes the filter and its effect junctions" {
        val client = createLedStripClientEntity(clientRepository, "Office", "192.168.1.52", 54, 55)
        val strip = saveLedStrip(stripRepository, client, "Strip C", 60, PiClientPin.D10.pinName, 100)
        val effect = saveLightEffect(effectRepository, objectMapper, settingsRepository, strip)
        val filter = saveFilter("Disposable filter")
        junctionRepository.save(LightEffectFilterJunctionEntity(filter = filter, effect = effect))

        filterApiService.deleteFilter(filter.uuid)

        filterRepository.findByUuid(filter.uuid).isEmpty shouldBe true
        junctionRepository.findByFilter(filter).isEmpty() shouldBe true
    }
})
