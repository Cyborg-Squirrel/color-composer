package io.cyborgsquirrel.clients.discovery.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.discovery.enums.DiscoveryJobStatus
import io.cyborgsquirrel.clients.discovery.job.ClientDiscoveryJob
import io.cyborgsquirrel.clients.discovery.model.DiscoveredClientsResponseList
import io.cyborgsquirrel.clients.discovery.model.DiscoveryStatusResponse
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.requests.SelectClientRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskScheduler
import java.time.Duration
import java.util.*

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
            DiscoveryJobStatus.complete, DiscoveryJobStatus.inProgress -> HttpResponse.ok(
                DiscoveredClientsResponseList(
                    discoveryJob.getDiscoveryResponses()
                )
            )

            DiscoveryJobStatus.error -> HttpResponse.badRequest("Client discovery encountered an error. Please try again.")
            DiscoveryJobStatus.idle -> HttpResponse.badRequest("Client discovery not started. Please start discovery before querying discovered clients.")
        }
    }

    @Get("/discovery-status")
    fun discoveryStatus(): HttpResponse<DiscoveryStatusResponse> {
        return HttpResponse.ok(DiscoveryStatusResponse(discoveryJob.getStatus()))
    }

    @Post("/confirm-client")
    fun confirm(selectedClient: SelectClientRequest): HttpResponse<Any> {
        val discoveredClients = discoveryJob.getDiscoveryResponses()
        val matchingClient =
            discoveredClients.firstOrNull { it.name == selectedClient.name && it.address == selectedClient.address }
        if (matchingClient != null) {
            discoveryJob.markIdle()
            for (client in discoveredClients) {
                val clientExists = clientRepository.findByAddress(matchingClient.address).isPresent
                if (!clientExists) {
                    val entity = LedStripClientEntity(
                        name = client.name,
                        address = client.address,
                        wsPort = client.wsPort,
                        apiPort = client.apiPort,
                        uuid = UUID.randomUUID().toString()
                    )
                    clientRepository.save(entity)
                    return HttpResponse.created(matchingClient)
                }
            }
        }

        return HttpResponse.badRequest("${selectedClient.name} did not match any of the discovered clients.")
    }
}