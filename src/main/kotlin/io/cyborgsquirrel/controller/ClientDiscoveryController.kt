package io.cyborgsquirrel.controller

import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.job.ClientDiscoveryJob
import io.cyborgsquirrel.job.enums.DiscoveryJobStatus
import io.cyborgsquirrel.model.requests.discovery.SelectClientsRequest
import io.cyborgsquirrel.model.responses.discovery.DiscoveryStatusResponse
import io.cyborgsquirrel.repository.H2LedStripClientRepository
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskScheduler
import java.time.Duration

@Controller
class ClientDiscoveryController(
    private val taskScheduler: TaskScheduler,
    private val discoveryJob: ClientDiscoveryJob,
    private val clientRepository: H2LedStripClientRepository,
) {

    @Post("/discover-clients")
    fun discover(): HttpResponse<Any> {
        if (discoveryJob.getStatus() != DiscoveryJobStatus.idle) {
            return HttpResponse.badRequest("Cannot start a new discovery while another is in progress")
        }

        taskScheduler.schedule(Duration.ofMillis(0), discoveryJob)
        return HttpResponse.ok()
    }

    @Post("/cancel-discovery")
    fun cancelDiscovery(): HttpResponse<Any> {
        discoveryJob.cancel()
        return HttpResponse.ok()
    }

    @Get("/discovered-clients")
    fun discoveredClients(): HttpResponse<Any> {
        val status = discoveryJob.getStatus()

        return when (status) {
            DiscoveryJobStatus.complete, DiscoveryJobStatus.inProgress -> HttpResponse.ok(discoveryJob.getDiscoveryResponses())
            DiscoveryJobStatus.error -> HttpResponse.badRequest("Client discovery encountered an error. Please try again.")
            DiscoveryJobStatus.idle -> HttpResponse.badRequest("Client discovery not started. Please start discovery before querying discovered clients.")
        }
    }

    @Get("/discovery-status")
    fun discoveryStatus(): HttpResponse<DiscoveryStatusResponse> {
        return HttpResponse.ok(DiscoveryStatusResponse(discoveryJob.getStatus()))
    }

    @Post("/confirm-clients")
    fun confirm(request: SelectClientsRequest): HttpResponse<Any> {
        val discoveredClients = discoveryJob.getDiscoveryResponses()
        var validRequest = true
        for (selectedClient in request.clients) {
            if (discoveredClients.firstOrNull { it.name == selectedClient.name && it.address == selectedClient.address } == null) {
                validRequest = false
                break
            }
        }

        if (validRequest) {
            discoveryJob.markIdle()

            for (client in discoveredClients) {
                val isNewClient = clientRepository.findByAddress(client.address).isPresent
                if (!isNewClient) {
                    val entity = LedStripClientEntity(
                        name = client.name,
                        address = client.address,
                        wsPort = client.wsPort,
                        apiPort = client.apiPort,
                    )
                    clientRepository.save(entity)
                }
            }

            return HttpResponse.ok()
        }

        return HttpResponse.badRequest("One or more clients in the request did not match the discovered clients.")
    }
}