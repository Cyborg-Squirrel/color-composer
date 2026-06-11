package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.repository.LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.led_strips.repository.PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.repository.LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_settings.repository.LightEffectSettingsRepository
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.api.EffectApi
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.LightEffectStatusCommand
import io.cyborgsquirrel.lighting.effects.requests.ReassignEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetPoolEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.objectToMap
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

/**
 * Verifies the HTTP wiring of the EffectController: status code mapping, query-parameter
 * routing, and that each endpoint is reachable at its declared path. Business behavior is
 * covered by EffectApiServiceTest.
 */
@MicronautTest
class EffectControllerTest(
    @param:Client private val apiClient: EffectApi,
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val poolRepository: LedStripPoolRepository,
    private val poolMemberRepository: PoolMemberLedStripRepository,
    private val effectRepository: LightEffectRepository,
    private val settingsRepository: LightEffectSettingsRepository,
    private val paletteRepository: LightEffectPaletteRepository,
    private val objectMapper: ObjectMapper,
) : StringSpec({

    afterEach {
        effectRepository.deleteAll()
        paletteRepository.deleteAll()
        settingsRepository.deleteAll()
        poolMemberRepository.deleteAll()
        poolRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    fun makeStripCreateRequest(stripUuid: String, name: String = "E") = CreateEffectRequest(
        stripUuid = stripUuid,
        poolUuid = null,
        effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
        name = name,
        settings = objectToMap(objectMapper, NightriderColorFillEffectSettings()),
        paletteUuid = null,
        settingsUuid = null,
    )

    // -----------------------------------------------------------------
    // Happy-path status mappings
    // -----------------------------------------------------------------

    "POST /effect returns 201 with the uuid body" {
        val client = createLedStripClientEntity(clientRepository, "Create lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 80)

        val response = apiClient.createEffect(makeStripCreateRequest(strip.uuid, "Created"))
        response.status shouldBe HttpStatus.CREATED
        (response.body() as String).isNotBlank() shouldBe true
    }

    "GET /effect/{uuid} returns 200 with the matching effect" {
        val client = createLedStripClientEntity(clientRepository, "Read lights", "192.168.50.51", 51, 52)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        val effectUuid = apiClient.createEffect(makeStripCreateRequest(strip.uuid, "Get Test")).body() as String

        val getResponse = apiClient.getEffect(effectUuid)
        getResponse.status shouldBe HttpStatus.OK
        val body = getResponse.body() as GetStripEffectResponse
        body.uuid shouldBe effectUuid
        body.stripUuid shouldBe strip.uuid
    }

    "PATCH /effect/update/{uuid} returns 204" {
        val client = createLedStripClientEntity(clientRepository, "Update lights", "192.168.50.52", 52, 53)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        val effectUuid = apiClient.createEffect(makeStripCreateRequest(strip.uuid, "Before")).body() as String

        val updateResponse = apiClient.updateEffect(
            effectUuid,
            UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = "After"),
        )
        updateResponse.status shouldBe HttpStatus.NO_CONTENT
        effectRepository.findByUuid(effectUuid).get().name shouldBe "After"
    }

    "PATCH /effect/reassign/{uuid} returns 204" {
        val client = createLedStripClientEntity(clientRepository, "Reassign lights", "192.168.50.53", 53, 54)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 100, PiClientPin.D21.pinName, 100)
        val effectUuid = apiClient.createEffect(makeStripCreateRequest(stripA.uuid, "Mover")).body() as String

        val reassignResponse = apiClient.reassignEffect(
            effectUuid,
            ReassignEffectRequest(unassign = false, targetStripUuid = stripB.uuid, targetPoolUuid = null),
        )
        reassignResponse.status shouldBe HttpStatus.NO_CONTENT
        effectRepository.findByUuid(effectUuid).get().strip?.uuid shouldBe stripB.uuid
    }

    "DELETE /effect/{uuid} returns 204" {
        val client = createLedStripClientEntity(clientRepository, "Delete lights", "192.168.50.54", 54, 55)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        val effectUuid = apiClient.createEffect(makeStripCreateRequest(strip.uuid, "Doomed")).body() as String

        val deleteResponse = apiClient.deleteEffect(effectUuid)
        deleteResponse.status shouldBe HttpStatus.NO_CONTENT
        effectRepository.findByUuid(effectUuid).isPresent shouldBe false
    }

    "POST /effect/command returns 204 and updates effect status" {
        val client = createLedStripClientEntity(clientRepository, "Command lights", "192.168.50.55", 55, 56)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        val effectUuid = apiClient.createEffect(makeStripCreateRequest(strip.uuid, "Status target")).body() as String

        val response = apiClient.mediaCommand(
            UpdateEffectStatusRequest(uuids = listOf(effectUuid), command = LightEffectStatusCommand.Play),
        )
        response.status shouldBe HttpStatus.NO_CONTENT
        effectRepository.findByUuid(effectUuid).get().status shouldBe LightEffectStatus.Playing
    }

    // -----------------------------------------------------------------
    // GET /effect query-parameter routing
    // -----------------------------------------------------------------

    "GET /effect without filters returns 200 with every effect" {
        val client = createLedStripClientEntity(clientRepository, "All lights", "192.168.50.56", 56, 57)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        apiClient.createEffect(makeStripCreateRequest(strip.uuid, "One"))

        val response = apiClient.getEffects(null, null)
        response.status shouldBe HttpStatus.OK
        (response.body() as GetEffectsResponse).effects.size shouldBe 1
    }

    "GET /effect?stripUuid=... returns 200 with effects filtered by strip" {
        val client = createLedStripClientEntity(clientRepository, "Strip filter lights", "192.168.50.57", 57, 58)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 100, PiClientPin.D21.pinName, 100)
        apiClient.createEffect(makeStripCreateRequest(stripB.uuid, "On B"))

        apiClient.getEffects(stripA.uuid, null).let {
            it.status shouldBe HttpStatus.OK
            (it.body() as GetEffectsResponse).effects.isEmpty() shouldBe true
        }
        apiClient.getEffects(stripB.uuid, null).let {
            it.status shouldBe HttpStatus.OK
            val effects = (it.body() as GetEffectsResponse).effects
            effects.size shouldBe 1
            (effects.first() as GetStripEffectResponse).stripUuid shouldBe stripB.uuid
        }
    }

    "GET /effect?poolUuid=... returns 200 with effects filtered by pool" {
        val client = createLedStripClientEntity(clientRepository, "Pool filter lights", "192.168.50.58", 58, 59)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)
        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Pool A",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average,
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = strip, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )
        apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = null,
                poolUuid = pool.uuid,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Pool Effect",
                settings = objectToMap(objectMapper, NightriderColorFillEffectSettings()),
                paletteUuid = null,
                settingsUuid = null,
            )
        )

        val response = apiClient.getEffects(null, pool.uuid)
        response.status shouldBe HttpStatus.OK
        val effects = (response.body() as GetEffectsResponse).effects
        effects.size shouldBe 1
        (effects.first() as GetPoolEffectResponse).poolUuid shouldBe pool.uuid
    }

    "GET /effect with no matching results returns 200 and an empty list" {
        val response = apiClient.getEffects(null, null)
        response.status shouldBe HttpStatus.OK
        (response.body() as GetEffectsResponse).effects.isEmpty() shouldBe true
    }

    // -----------------------------------------------------------------
    // Schemas endpoint
    // -----------------------------------------------------------------

    "GET /effect/schemas returns 200 with one schema per effect" {
        val response = apiClient.getSchemas()
        response.status shouldBe HttpStatus.OK
        (response.body() as List<*>).size shouldBe 8
    }

    // -----------------------------------------------------------------
    // Service exception propagation
    // -----------------------------------------------------------------

    "getEffect throws ResourceNotFoundException for a non-existent effect" {
        shouldThrow<ResourceNotFoundException> {
            apiClient.getEffect(UUID.randomUUID().toString())
        }
    }

    "createEffect throws ClientRequestException when no owner is specified" {
        val request = CreateEffectRequest(
            stripUuid = null,
            poolUuid = null,
            effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
            name = "Invalid",
            settings = emptyMap(),
            paletteUuid = null,
            settingsUuid = null,
        )
        shouldThrow<ClientRequestException> {
            apiClient.createEffect(request)
        }
    }
})
