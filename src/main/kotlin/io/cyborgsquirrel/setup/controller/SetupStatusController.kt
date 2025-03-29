package io.cyborgsquirrel.setup.controller

import io.cyborgsquirrel.setup.responses.status.SetupStatusResponse
import io.cyborgsquirrel.setup.service.SetupStatusCheckService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.LoggerFactory

@Controller
class SetupStatusController(private val setupStatusCheckService: SetupStatusCheckService) {

    @Get("/setup-status")
    fun setupStatus(): HttpResponse<Any> {
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

    companion object {
        private val logger = LoggerFactory.getLogger(SetupStatusController::class.java)
    }
}