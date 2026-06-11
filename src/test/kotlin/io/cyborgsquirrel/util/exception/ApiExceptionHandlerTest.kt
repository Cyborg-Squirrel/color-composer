package io.cyborgsquirrel.util.exception

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.mockk.mockk

/**
 * Unit tests for the global [io.micronaut.http.server.exceptions.ExceptionHandler]s that map
 * service exceptions to consistent HTTP responses with an [ApiErrorResponse] body. These handlers
 * are not exercised by the in-process declarative @Client controller tests (those surface the
 * thrown exception directly), so they are verified here in isolation.
 */
class ApiExceptionHandlerTest : StringSpec({

    val request = mockk<HttpRequest<*>>()

    "ClientRequestExceptionHandler maps to 400 with the exception message" {
        val handler = ClientRequestExceptionHandler()

        val response = handler.handle(request, ClientRequestException("Invalid request"))

        response.status shouldBe HttpStatus.BAD_REQUEST
        response.body() shouldNotBe null
        response.body()!!.message shouldBe "Invalid request"
    }

    "ResourceNotFoundExceptionHandler maps to 404 with the exception message" {
        val handler = ResourceNotFoundExceptionHandler()

        val response = handler.handle(request, ResourceNotFoundException("Client with uuid abc doesn't exist!"))

        response.status shouldBe HttpStatus.NOT_FOUND
        response.body() shouldNotBe null
        response.body()!!.message shouldBe "Client with uuid abc doesn't exist!"
    }

    "ServerErrorExceptionHandler maps to 500 with the exception message" {
        val handler = ServerErrorExceptionHandler()

        val response = handler.handle(request, ServerErrorException("Something went wrong"))

        response.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        response.body() shouldNotBe null
        response.body()!!.message shouldBe "Something went wrong"
    }
})
