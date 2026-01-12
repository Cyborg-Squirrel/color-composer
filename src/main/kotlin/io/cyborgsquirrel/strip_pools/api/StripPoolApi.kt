package io.cyborgsquirrel.strip_pools.api

import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolMembersRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface StripPoolApi {
    @Get
    fun getPools(): HttpResponse<Any>

    @Get("/{uuid}")
    fun getPool(uuid: String): HttpResponse<Any>

    @Post
    fun createPool(@Body request: CreateStripPoolRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updatePool(uuid: String, @Body request: UpdateStripPoolRequest): HttpResponse<Any>

    @Patch("/{uuid}/members")
    fun updatePoolMembers(uuid: String, @Body request: UpdateStripPoolMembersRequest): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deletePool(uuid: String): HttpResponse<Any>
}