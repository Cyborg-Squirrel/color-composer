package io.cyborgsquirrel.util.exception

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

/**
 * Maps [ResourceNotFoundException] thrown by services to a 404 Not Found response.
 */
@Produces
@Singleton
class ResourceNotFoundExceptionHandler :
    ExceptionHandler<ResourceNotFoundException, HttpResponse<ApiErrorResponse>> {

    override fun handle(
        request: HttpRequest<*>,
        exception: ResourceNotFoundException
    ): HttpResponse<ApiErrorResponse> {
        return HttpResponse.notFound(
            ApiErrorResponse(exception.message ?: "")
        )
    }
}
