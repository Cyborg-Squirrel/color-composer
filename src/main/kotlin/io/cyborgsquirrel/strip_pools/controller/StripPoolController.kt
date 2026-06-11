package io.cyborgsquirrel.strip_pools.controller

import io.cyborgsquirrel.strip_pools.api.StripPoolApi
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolMembersRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.services.StripPoolApiService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller

@Controller("/pool")
class StripPoolController(
    private val stripPoolApiService: StripPoolApiService,
) : StripPoolApi {

    override fun getPools(): HttpResponse<Any> {
        return HttpResponse.ok(stripPoolApiService.getStripPools())
    }

    override fun getPool(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(stripPoolApiService.getStripPool(uuid))
    }

    override fun createPool(@Body request: CreateStripPoolRequest): HttpResponse<Any> {
        return HttpResponse.created(stripPoolApiService.createStripPool(request))
    }

    override fun updatePool(uuid: String, @Body request: UpdateStripPoolRequest): HttpResponse<Any> {
        stripPoolApiService.updateStripPool(uuid, request)
        return HttpResponse.noContent()
    }

    override fun updatePoolMembers(uuid: String, @Body request: UpdateStripPoolMembersRequest): HttpResponse<Any> {
        stripPoolApiService.updatePoolMembers(uuid, request)
        return HttpResponse.noContent()
    }

    override fun deletePool(uuid: String): HttpResponse<Any> {
        stripPoolApiService.deletePool(uuid)
        return HttpResponse.noContent()
    }
}
