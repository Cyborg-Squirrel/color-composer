package io.cyborgsquirrel.led_strips.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.api.LedStripSetupApi
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.led_strips.responses.GetLedStripResponse
import io.cyborgsquirrel.led_strips.responses.GetLedStripsResponse
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.test_helpers.saveLightEffect
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class LedStripSetupControllerTest(
    @Client private val apiClient: LedStripSetupApi,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val objectMapper: ObjectMapper,
) :
    StringSpec({

        afterTest {
            stripRepository.deleteAll()
            clientRepository.deleteAll()
        }

        "Requesting LED strips" {
            // Request strip which doesn't exist - bad request
            var response = apiClient.getStrip(UUID.randomUUID().toString())
            response.status shouldBe HttpStatus.BAD_REQUEST

            // Request strips for a client which doesn't exist - bad request
            response = apiClient.getStrips(UUID.randomUUID().toString())
            response.status shouldBe HttpStatus.BAD_REQUEST

            // Request strips for a client which has no strips
            val client = createLedStripClientEntity(clientRepository, "Lamp lights", "192.168.1.23", 90, 91)
            response = apiClient.getStrips(client.uuid!!)
            response.status shouldBe HttpStatus.OK
            var getStripsResponse = response.body() as GetLedStripsResponse
            getStripsResponse.strips.isEmpty() shouldBe true

            // Request a single strip
            val strip = saveLedStrip(stripRepository, client, "Strip A", 50, PiClientPin.D21.pinName, 100)
            response = apiClient.getStrip(strip.uuid!!)
            response.status shouldBe HttpStatus.OK
            val getStripResponse = response.body() as GetLedStripResponse
            getStripResponse.name shouldBe strip.name
            getStripResponse.uuid shouldBe strip.uuid
            getStripResponse.length shouldBe strip.length
            getStripResponse.height shouldBe strip.height
            getStripResponse.powerLimit shouldBe strip.powerLimit
            getStripResponse.blendMode shouldBe strip.blendMode
            getStripResponse.brightness shouldBe strip.brightness

            // Request strips for client
            response = apiClient.getStrips(client.uuid!!)
            response.status shouldBe HttpStatus.OK
            getStripsResponse = response.body() as GetLedStripsResponse
            getStripsResponse.strips.size shouldBe 1
            getStripsResponse.strips.first().name shouldBe strip.name
            getStripsResponse.strips.first().uuid shouldBe strip.uuid
            getStripsResponse.strips.first().pin shouldBe strip.pin
            getStripsResponse.strips.first().length shouldBe strip.length
            getStripsResponse.strips.first().height shouldBe strip.height
            getStripsResponse.strips.first().powerLimit shouldBe strip.powerLimit
            getStripsResponse.strips.first().blendMode shouldBe strip.blendMode
            getStripsResponse.strips.first().brightness shouldBe strip.brightness
        }

        "Creating LED strips" {
            // Create without optional fields specified
            val client = createLedStripClientEntity(clientRepository, "Porch lights", "192.168.50.50", 50, 51)
            var request =
                CreateLedStripRequest(
                    client.uuid!!,
                    "Porch underglow strip",
                    "D10",
                    240,
                    blendMode = BlendMode.Additive,
                )
            var response = apiClient.createStrip(request)

            response.status shouldBe HttpStatus.CREATED
            var uuid = response.body() as String
            var stripOptional = stripRepository.findByUuid(uuid)
            stripOptional.isPresent shouldBe true
            stripOptional.get().uuid shouldBe uuid
            stripOptional.get().name shouldBe request.name
            stripOptional.get().length shouldBe request.length
            stripOptional.get().height shouldBe 1
            stripOptional.get().blendMode shouldBe request.blendMode
            stripOptional.get().powerLimit shouldBe null
            // Default brightness for 240 LEDs at 500mA
            stripOptional.get().brightness shouldBe 3

            stripRepository.deleteAll()

            // Create with optional fields specified
            request =
                CreateLedStripRequest(
                    client.uuid!!,
                    "Porch under-glow strip",
                    "D10",
                    180,
                    2,
                    4000,
                    60,
                    BlendMode.Average
                )
            response = apiClient.createStrip(request)
            response.status shouldBe HttpStatus.CREATED
            uuid = response.body() as String
            stripOptional = stripRepository.findByUuid(uuid)
            stripOptional.isPresent shouldBe true
            stripOptional.get().uuid shouldBe uuid
            stripOptional.get().name shouldBe request.name
            stripOptional.get().length shouldBe request.length
            stripOptional.get().height shouldBe request.height
            stripOptional.get().blendMode shouldBe request.blendMode
            stripOptional.get().powerLimit shouldBe request.powerLimit
            stripOptional.get().brightness shouldBe request.brightness
        }

        "Updating strips" {
            val client = createLedStripClientEntity(clientRepository, "Porch lights", "192.168.50.50", 50, 51)
            val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 100)
            val newBlendMode = if (strip.blendMode == BlendMode.Additive) BlendMode.Average else BlendMode.Additive
            val request = UpdateLedStripRequest(
                name = "Ceiling strip",
                pin = null,
                length = 180,
                height = 3,
                blendMode = newBlendMode,
                powerLimit = 10000,
                brightness = 85,
            )

            val response = apiClient.updateStrip(strip.uuid!!, request)
            response.status shouldBe HttpStatus.NO_CONTENT

            val updatedStripOptional = stripRepository.findByUuid(strip.uuid!!)
            updatedStripOptional.isPresent shouldBe true
            updatedStripOptional.get().name shouldBe request.name
            updatedStripOptional.get().length shouldBe request.length
            updatedStripOptional.get().height shouldBe request.height
            updatedStripOptional.get().blendMode shouldBe request.blendMode
            updatedStripOptional.get().powerLimit shouldBe request.powerLimit
            updatedStripOptional.get().brightness shouldBe request.brightness
        }

        "Deleting strips" {
            // Deleting a strip which doesn't exist - bad request
            var response = apiClient.deleteStrip(UUID.randomUUID().toString())
            response.status shouldBe HttpStatus.BAD_REQUEST

            val client = createLedStripClientEntity(clientRepository, "Porch lights", "192.168.50.50", 50, 51)
            var strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 100)

            // Deleting a strip which does exist and has no effects
            response = apiClient.deleteStrip(strip.uuid!!)
            response.status shouldBe HttpStatus.NO_CONTENT
            var stripOptional = stripRepository.findByUuid(strip.uuid!!)
            stripOptional.isPresent shouldBe false

            // Deleting a strip which has a light effect - bad request
            strip = saveLedStrip(stripRepository, client, "Strip B", 144, PiClientPin.D21.pinName, 80)
            saveLightEffect(effectRepository, objectMapper, strip)
            response = apiClient.deleteStrip(UUID.randomUUID().toString())
            response.status shouldBe HttpStatus.BAD_REQUEST

            effectRepository.deleteAll()

            // Deleting the strip after deleting the light effect
            response = apiClient.deleteStrip(strip.uuid!!)
            response.status shouldBe HttpStatus.NO_CONTENT
            stripOptional = stripRepository.findByUuid(strip.uuid!!)
            stripOptional.isPresent shouldBe false
        }
    })