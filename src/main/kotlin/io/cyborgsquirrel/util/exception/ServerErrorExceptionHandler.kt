package io.cyborgsquirrel.util.exception

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
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
        return HttpResponse.serverError(
            ApiErrorResponse(exception.message?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred")
        )
    }
}
