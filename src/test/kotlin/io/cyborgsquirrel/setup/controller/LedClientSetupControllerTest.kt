package io.cyborgsquirrel.setup.controller

import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.setup.api.LedClientSetupApi
import io.cyborgsquirrel.setup.requests.client.CreateClientRequest
import io.cyborgsquirrel.setup.requests.client.UpdateClientRequest
import io.cyborgsquirrel.setup.responses.client.GetClientResponse
import io.cyborgsquirrel.setup.responses.client.GetClientsResponse
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class LedClientSetupControllerTest(
    @Client("/") private val apiClient: LedClientSetupApi,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
) :
    StringSpec({

        afterTest {
            clientRepository.deleteAll()
        }

        "Requesting clients" {
            var response = apiClient.getClient(UUID.randomUUID().toString())
            response.status shouldBe HttpStatus.BAD_REQUEST

            response = apiClient.getAllClients()
            response.status shouldBe HttpStatus.OK
            var clientListResponse = response.body() as GetClientsResponse
            clientListResponse.clients.isEmpty() shouldBe true

            val client = createLedStripClientEntity(clientRepository, "Window lights", "192.168.1.53", 33, 44)

            response = apiClient.getClient(UUID.randomUUID().toString())
            response.status shouldBe HttpStatus.BAD_REQUEST

            response = apiClient.getClient(client.uuid!!)
            response.status shouldBe HttpStatus.OK
            val singleClientResponse = response.body() as GetClientResponse
            singleClientResponse.uuid shouldBe client.uuid
            singleClientResponse.name shouldBe client.name
            singleClientResponse.address shouldBe client.address
            singleClientResponse.wsPort shouldBe client.wsPort
            singleClientResponse.apiPort shouldBe client.apiPort

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
            val createClientRequest = CreateClientRequest("Window lights", "192.168.5.5", 80, 82)
            val createResponse = apiClient.create(createClientRequest)
            createResponse.status shouldBe HttpStatus.CREATED

            val clientUuid = createResponse.body() as String
            val clientEntityOptional = clientRepository.findByUuid(clientUuid)
            clientEntityOptional.isPresent shouldBe true

            val clientEntity = clientEntityOptional.get()
            clientEntity.uuid shouldBe clientUuid
            clientEntity.name shouldBe createClientRequest.name
            clientEntity.address shouldBe createClientRequest.address
            clientEntity.apiPort shouldBe createClientRequest.apiPort
            clientEntity.wsPort shouldBe createClientRequest.wsPort
        }

        "Updating clients" {
            val clientEntity = createLedStripClientEntity(clientRepository, "Window lights", "192.168.1.112", 112, 113)
            val updatedClientRequest = UpdateClientRequest("Living room lights", "192.168.1.113", 115, 116)
            val updateResponse = apiClient.update(clientEntity.uuid!!, updatedClientRequest)
            updateResponse.status shouldBe HttpStatus.NO_CONTENT

            val clientEntityOptional = clientRepository.findByUuid(clientEntity.uuid!!)
            clientEntityOptional.isPresent shouldBe true
            val updatedClientEntity = clientEntityOptional.get()
            updatedClientEntity.name shouldBe updatedClientRequest.name
            updatedClientEntity.address shouldBe updatedClientRequest.address
            updatedClientEntity.apiPort shouldBe updatedClientRequest.apiPort
            updatedClientEntity.wsPort shouldBe updatedClientRequest.wsPort
            updatedClientEntity.uuid shouldBe clientEntity.uuid
        }
    })