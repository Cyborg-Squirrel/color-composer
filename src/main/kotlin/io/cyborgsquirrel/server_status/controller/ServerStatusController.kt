package io.cyborgsquirrel.server_status.controller

import io.cyborgsquirrel.server_status.api.ServerStatusApi
import io.cyborgsquirrel.server_status.responses.SetupStatusResponse
import io.cyborgsquirrel.server_status.service.SetupStatusCheckService
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import org.slf4j.LoggerFactory

@Controller
class ServerStatusController(private val setupStatusCheckService: SetupStatusCheckService) : ServerStatusApi {

    @Property(name = "cc-version", defaultValue = "")
    private lateinit var version: String

    override fun setupStatus(): HttpResponse<Any> {
        try {
            val setupStatus = setupStatusCheckService.getSetupStatus()
            return HttpResponse.ok(SetupStatusResponse(setupStatus))
        } catch (ex: Exception) {
            logger.error("Exception during setup status check: ${ex.stackTraceToString()}")
            return HttpResponse.serverError("Could not determine server setup status")
        } catch (er: Error) {
            logger.error("Error during setup status check: ${er.stackTraceToString()}")
            return HttpResponse.serverError("Could not determine server setup status")
        }
    }

    override fun getVersion(): HttpResponse<String> = HttpResponse.ok(version)

    companion object {
        private val logger = LoggerFactory.getLogger(ServerStatusController::class.java)
    }
}