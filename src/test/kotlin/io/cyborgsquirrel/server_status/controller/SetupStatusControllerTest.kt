package io.cyborgsquirrel.server_status.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.server_status.api.ServerStatusApi
import io.cyborgsquirrel.server_status.responses.SetupStatus
import io.cyborgsquirrel.server_status.responses.SetupStatusResponse
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.test_helpers.saveLightEffect
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class SetupStatusControllerTest(
    @Client private val apiClient: ServerStatusApi,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val objectMapper: ObjectMapper
) :
    StringSpec({

        afterTest {
            stripRepository.deleteAll()
            clientRepository.deleteAll()
            effectRepository.deleteAll()
        }

        "Setup status - nothing setup" {
            val statusResponse = apiClient.setupStatus()
            statusResponse.status shouldBe HttpStatus.OK
            val response = statusResponse.body() as SetupStatusResponse
            response.status shouldBe SetupStatus.NoClients
        }

        "Setup status - clients configured, no strips" {
            createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.1.12", 1111, 2222)
            val statusResponse = apiClient.setupStatus()
            statusResponse.status shouldBe HttpStatus.OK
            val response = statusResponse.body() as SetupStatusResponse
            response.status shouldBe SetupStatus.NoStrips
        }

        "Setup status - clients and strips configured, no effects" {
            val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.1.12", 1111, 2222)
            saveLedStrip(stripRepository, client, "Hallway lights", 240, PiClientPin.D21.pinName)
            val statusResponse = apiClient.setupStatus()
            val response = statusResponse.body() as SetupStatusResponse
            response.status shouldBe SetupStatus.NoEffects
        }

        "Setup status - everything configured" {
            val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.1.12", 1111, 2222)
            val strip = saveLedStrip(stripRepository, client, "Hallway lights", 240, PiClientPin.D10.pinName)
            saveLightEffect(effectRepository, objectMapper, strip)
            val statusResponse = apiClient.setupStatus()
            val response = statusResponse.body() as SetupStatusResponse
            response.status shouldBe SetupStatus.SetupComplete
        }
    })