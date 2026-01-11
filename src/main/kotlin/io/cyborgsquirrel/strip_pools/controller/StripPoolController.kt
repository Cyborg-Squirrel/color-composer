package io.cyborgsquirrel.strip_pools.controller

import io.cyborgsquirrel.strip_pools.api.StripPoolApi
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.services.StripPoolApiService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller

@Controller("/pool")
class StripPoolController(
    private val stripPoolApiService: StripPoolApiService,
) : StripPoolApi {

    override fun getPools(): HttpResponse<Any> {
        return try {
            val strips = stripPoolApiService.getStripPools()
            HttpResponse.ok(strips)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun getPool(uuid: String): HttpResponse<Any> {
        return try {
            val strip = stripPoolApiService.getStripPool(uuid)
            HttpResponse.ok(strip)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun createPool(@Body request: CreateStripPoolRequest): HttpResponse<Any> {
        return try {
            val stripUuid = stripPoolApiService.createStripPool(request)
            return HttpResponse.created(stripUuid)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun updatePool(uuid: String, @Body request: UpdateStripPoolRequest): HttpResponse<Any> {
        return try {
            stripPoolApiService.updateStripPool(uuid, request)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun updatePoolMembers(uuid: String, @Body request: UpdateStripPoolRequest): HttpResponse<Any> {
        return try {
            stripPoolApiService.updatePoolMembers(uuid, request)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun deletePool(uuid: String): HttpResponse<Any> {
        return try {
            stripPoolApiService.deletePool(uuid)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }
}