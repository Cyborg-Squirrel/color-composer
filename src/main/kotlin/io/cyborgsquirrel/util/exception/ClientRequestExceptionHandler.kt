package io.cyborgsquirrel.util.exception

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

/**
 * Maps [ClientRequestException] thrown by services to a 400 Bad Request response.
 */
@Produces
@Singleton
class ClientRequestExceptionHandler :
    ExceptionHandler<ClientRequestException, HttpResponse<ApiErrorResponse>> {

    override fun handle(
        request: HttpRequest<*>,
        exception: ClientRequestException
    ): HttpResponse<ApiErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        return HttpResponse.badRequest(
            ApiErrorResponse(status.code, status.reason, exception.message ?: "")
        )
    }
}
