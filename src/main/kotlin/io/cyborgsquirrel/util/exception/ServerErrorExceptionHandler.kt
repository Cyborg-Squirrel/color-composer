package io.cyborgsquirrel.util.exception

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

/**
 * Maps [ServerErrorException] thrown by services to a 500 Internal Server Error response.
 */
@Produces
@Singleton
class ServerErrorExceptionHandler :
    ExceptionHandler<ServerErrorException, HttpResponse<ApiErrorResponse>> {

    override fun handle(
        request: HttpRequest<*>,
        exception: ServerErrorException
    ): HttpResponse<ApiErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        return HttpResponse.serverError(
            ApiErrorResponse(status.code, status.reason, exception.message ?: "")
        )
    }
}
