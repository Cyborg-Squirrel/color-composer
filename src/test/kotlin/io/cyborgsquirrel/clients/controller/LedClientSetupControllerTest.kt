package io.cyborgsquirrel.clients.controller

import io.cyborgsquirrel.clients.api.LedClientApi
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.clients.responses.GetClientsResponse
import io.cyborgsquirrel.clients.status.ClientStatusInfo
import io.cyborgsquirrel.clients.status.ClientStatusService
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import java.util.*

@MicronautTest
class LedClientSetupControllerTest(
    @param:Client private val apiClient: LedClientApi,
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val clientStatusService: ClientStatusService
) :
    StringSpec({
        afterTest {
            clientRepository.deleteAll()
        }

        "Requesting clients" {
            shouldThrow<ResourceNotFoundException> {
                apiClient.getClient(UUID.randomUUID().toString())
            }

            var response = apiClient.getAllClients()
            response.status shouldBe HttpStatus.OK
            var clientListResponse = response.body() as GetClientsResponse
            clientListResponse.clients.isEmpty() shouldBe true

            val client = createLedStripClientEntity(clientRepository, "Window lights", "192.168.1.53", 33, 44)

            val mockClientStatusService = getMock(clientStatusService)
            val mockedStatus = ClientStatus.SetupIncomplete
            every {
                mockClientStatusService.getStatusForClient(client)
            } returns Optional.of(ClientStatusInfo.inactive(mockedStatus))

            shouldThrow<ResourceNotFoundException> {
                apiClient.getClient(UUID.randomUUID().toString())
            }

            response = apiClient.getClient(client.uuid)
            response.status shouldBe HttpStatus.OK
            val singleClientResponse = response.body() as GetClientResponse
            singleClientResponse.uuid shouldBe client.uuid
            singleClientResponse.name shouldBe client.name
            singleClientResponse.address shouldBe client.address
            singleClientResponse.wsPort shouldBe client.wsPort
            singleClientResponse.apiPort shouldBe client.apiPort
            singleClientResponse.status shouldBe mockedStatus
            singleClientResponse.firmwareVersion shouldBe client.firmwareVersion

            response = apiClient.getAllClients()
            response.status shouldBe HttpStatus.OK
            clientListResponse = response.body() as GetClientsResponse
            clientListResponse.clients.isNotEmpty() shouldBe true
            clientListResponse.clients.size shouldBe 1
            clientListResponse.clients.first().uuid shouldBe client.uuid
            clientListResponse.clients.first().name shouldBe client.name
            clientListResponse.clients.first().address shouldBe client.address
            clientListResponse.clients.first().wsPort shouldBe client.wsPort
            clientListResponse.clients.first().apiPort shouldBe client.apiPort
        }

        "Creating clients" {
            // createClient builds the GET-shaped client object for the LedClientCreated event, which reads client status.
            val mockClientStatusService = getMock(clientStatusService)
            every {
                mockClientStatusService.getStatusForClient(any())
            } returns Optional.of(ClientStatusInfo.inactive(ClientStatus.SetupIncomplete))

            val createClientRequest =
                CreateClientRequest("Window lights", "192.168.5.5", ClientType.Pi, ColorOrder.GRB, 80, 82, 500)
            val createResponse = apiClient.create(createClientRequest)
            createResponse.status shouldBe HttpStatus.CREATED

            val clientUuid = createResponse.body() as String
            val clientEntityOptional = clientRepository.findByUuid(clientUuid)
            clientEntityOptional.isPresent shouldBe true

            val clientEntity = clientEntityOptional.get()
            clientEntity.uuid shouldBe clientUuid
            clientEntity.name shouldBe createClientRequest.name
            clientEntity.address shouldBe createClientRequest.address
            clientEntity.clientType shouldBe createClientRequest.clientType
            clientEntity.colorOrder shouldBe createClientRequest.colorOrder
            clientEntity.apiPort shouldBe createClientRequest.apiPort
            clientEntity.wsPort shouldBe createClientRequest.wsPort
            clientEntity.powerLimit shouldBe createClientRequest.powerLimit
            clientEntity.firmwareVersion shouldBe LedStripClientEntity.DEFAULT_FIRMWARE_VERSION
        }

        "Updating clients" {
            val clientEntity = createLedStripClientEntity(clientRepository, "Window lights", "192.168.1.112", 112, 113)
            val updatedClientRequest =
                UpdateClientRequest("Living room lights", "192.168.1.113", ColorOrder.GRB, 115, 116, 333)
            val updateResponse = apiClient.update(clientEntity.uuid, updatedClientRequest)
            updateResponse.status shouldBe HttpStatus.NO_CONTENT

            val clientEntityOptional = clientRepository.findByUuid(clientEntity.uuid)
            clientEntityOptional.isPresent shouldBe true
            val updatedClientEntity = clientEntityOptional.get()
            updatedClientEntity.name shouldBe updatedClientRequest.name
            updatedClientEntity.address shouldBe updatedClientRequest.address
            updatedClientEntity.clientType shouldBe clientEntity.clientType
            updatedClientEntity.colorOrder shouldBe updatedClientRequest.colorOrder
            updatedClientEntity.apiPort shouldBe updatedClientRequest.apiPort
            updatedClientEntity.wsPort shouldBe updatedClientRequest.wsPort
            updatedClientEntity.uuid shouldBe clientEntity.uuid
            updatedClientEntity.powerLimit shouldBe updatedClientRequest.powerLimit
        }

        "Updating a client which doesn't exist - not found" {
            val updateRequest =
                UpdateClientRequest("Living room lights", "192.168.1.113", ColorOrder.GRB, 115, 116, 333)
            shouldThrow<ResourceNotFoundException> {
                apiClient.update(UUID.randomUUID().toString(), updateRequest)
            }
        }

        "Delete clients" {
            val clientEntity = createLedStripClientEntity(clientRepository, "Window lights", "192.168.50.67", 80, 90)
            val strip = saveLedStrip(stripRepository, clientEntity, "Window light", 60, PiClientPin.D10.pinName, 100)

            // Client still has a strip - deleting is a conflict (bad request)
            shouldThrow<ClientRequestException> {
                apiClient.deleteClient(clientEntity.uuid)
            }

            stripRepository.delete(strip)

            val deleteResponse = apiClient.deleteClient(clientEntity.uuid)
            deleteResponse.status shouldBe HttpStatus.NO_CONTENT

            // Client is already gone - not found
            shouldThrow<ResourceNotFoundException> {
                apiClient.deleteClient(clientEntity.uuid)
            }
        }
    }) {
    @MockBean(ClientStatusService::class)
    fun clientStatusService(): ClientStatusService = mockk()
}
