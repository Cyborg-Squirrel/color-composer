package io.cyborgsquirrel.led_strips.services

import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class LedStripApiServiceTest(
    private val ledStripApiService: LedStripApiService,
    private val stripRepository: H2LedStripRepository,
    private val clientRepository: H2LedStripClientRepository
) : StringSpec({

    afterTest {
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "getStrip should throw exception for non-existent strip" {
        shouldThrow<ClientRequestException> {
            ledStripApiService.getStrip("non-existent-uuid")
        }
    }

    "getStrip should return strip by uuid" {
        val client = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.100", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Test Strip", 100, "D10", 100)

        val result = ledStripApiService.getStrip(strip.uuid!!)
        result.uuid shouldBe strip.uuid
        result.name shouldBe strip.name
        result.length shouldBe strip.length
        result.pin shouldBe strip.pin
        result.blendMode shouldBe strip.blendMode
        result.brightness shouldBe strip.brightness
    }

    "getStrips should return empty list when no strips exist" {
        val client = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.100", 50, 51)
        
        val result = ledStripApiService.getStrips(client.uuid!!)
        result.strips.isEmpty() shouldBe true
    }

    "getStrips should return all strips for a client" {
        val client = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.100", 50, 51)
        val strip1 = saveLedStrip(stripRepository, client, "Strip 1", 100, "D10", 100)
        val strip2 = saveLedStrip(stripRepository, client, "Strip 2", 150, "D12", 150)

        val result = ledStripApiService.getStrips(client.uuid!!)
        result.strips.size shouldBe 2
        val matchingStrip1 = result.strips.first { it.uuid == strip1.uuid }
        val matchingStrip2 = result.strips.first { it.uuid == strip2.uuid }

        matchingStrip1 shouldNotBe null
        matchingStrip2 shouldNotBe null

        result.strips.forEach { rs ->
            val matchingStrip = listOf(strip1, strip2).first { it.uuid == rs.uuid }
            matchingStrip.client!!.uuid shouldBe rs.clientUuid
            matchingStrip.name shouldBe rs.name
            matchingStrip.pin shouldBe rs.pin
            matchingStrip.length shouldBe rs.length
            matchingStrip.height shouldBe rs.height
            matchingStrip.brightness shouldBe rs.brightness
            matchingStrip.blendMode shouldBe rs.blendMode
        }
    }

    "createStrip should create new strip with valid request" {
        val client = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.100", 50, 51)
        
        val request = CreateLedStripRequest(
            client.uuid!!,
            "New Strip",
            "D10",
            200,
            blendMode = BlendMode.Additive
        )

        val uuid = ledStripApiService.createStrip(request)
        uuid shouldNotBe null
        uuid.isEmpty() shouldBe false

        val savedStrip = stripRepository.findByUuid(uuid)
        savedStrip.isPresent shouldBe true
        savedStrip.get().name shouldBe request.name
        savedStrip.get().length shouldBe request.length
        savedStrip.get().pin shouldBe request.pin
        savedStrip.get().blendMode shouldBe request.blendMode
    }

    "createStrip should fail for non-existent client" {
        val request = CreateLedStripRequest(
            "non-existent-uuid",
            "New Strip",
            "D10",
            200,
            blendMode = BlendMode.Additive
        )

        shouldThrow<ClientRequestException> {
            ledStripApiService.createStrip(request)
        }
    }

    "createStrip should fail for Pi client with existing strip" {
        val client = createLedStripClientEntity(clientRepository, "Pi Client", "192.168.1.100", 50, 51)
        client.clientType = ClientType.Pi
        
        // Create first strip for Pi client
        val firstStrip = saveLedStrip(stripRepository, client, "First Strip", 100, "D10", 100)
        
        val request = CreateLedStripRequest(
            client.uuid!!,
            "Second Strip",
            "D12",
            200,
            blendMode = BlendMode.Additive
        )

        shouldThrow<ClientRequestException> {
            ledStripApiService.createStrip(request)
        }
    }

    "updateStrip should update strip properties" {
        val client = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.100", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Original Strip", 100, "D10", 100)

        val request = UpdateLedStripRequest(
            name = "Updated Strip",
            pin = "D12",
            length = 150,
            blendMode = BlendMode.Average,
            height = null,
            brightness = strip.brightness,
            clientUuid = client.uuid
        )

        ledStripApiService.updateStrip(strip.uuid!!, request)

        val updatedStrip = stripRepository.findByUuid(strip.uuid!!)
        updatedStrip.get().name shouldBe request.name
        updatedStrip.get().length shouldBe request.length
        updatedStrip.get().pin shouldBe request.pin
        updatedStrip.get().blendMode shouldBe request.blendMode
    }

    "updateStrip should fail when moving to Pi client with existing strip" {
        val clientA = createLedStripClientEntity(clientRepository, "Pi Client", "192.168.1.100", 50, 51)
        val stripA = saveLedStrip(stripRepository, clientA, "Existing Strip", 100, "D10", 100)
        val clientB = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.101", 52, 53)
        val stripB = saveLedStrip(stripRepository, clientB, "Test Strip", 100, "D12", 100)

        val request = UpdateLedStripRequest(
            clientUuid = clientB.uuid!!,
            name = "Updated Strip",
            pin = "D12",
            length = 150,
            blendMode = BlendMode.Average,
            height = null,
            brightness = stripA.brightness,
        )

        shouldThrow<ClientRequestException> {
            ledStripApiService.updateStrip(stripA.uuid!!, request)
        }
    }

    "updateStrip should succeed when moving to Pi client with no existing strips" {
        val client = createLedStripClientEntity(clientRepository, "Pi Client", "192.168.1.100", 50, 51)
        client.clientType = ClientType.Pi
        
        val newClient = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.101", 52, 53)
        val strip = saveLedStrip(stripRepository, newClient, "Test Strip", 100, "D12", 100)

        val request = UpdateLedStripRequest(
            clientUuid = client.uuid!!,
            name = "Updated Strip",
            pin = "D10",
            length = 150,
            blendMode = BlendMode.Average,
            height = null,
            brightness = strip.brightness,
        )

        ledStripApiService.updateStrip(strip.uuid!!, request)

        val updatedStrip = stripRepository.findByUuid(strip.uuid!!)
        updatedStrip.get().client shouldBe client
        updatedStrip.get().pin shouldBe request.pin
    }

    "deleteStrip should delete existing strip" {
        val client = createLedStripClientEntity(clientRepository, "Test Client", "192.168.1.100", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Test Strip", 100, "D10", 100)

        ledStripApiService.onStripDeleted(strip.uuid!!)

        val deletedStrip = stripRepository.findByUuid(strip.uuid!!)
        deletedStrip.isEmpty shouldBe true
    }

    "deleteStrip should fail for non-existent strip" {
        shouldThrow<ClientRequestException> {
            ledStripApiService.onStripDeleted("non-existent-uuid")
        }
    }
})