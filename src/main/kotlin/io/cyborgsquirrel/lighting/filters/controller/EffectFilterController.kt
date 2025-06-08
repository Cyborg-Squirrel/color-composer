package io.cyborgsquirrel.lighting.filters.controller

import io.cyborgsquirrel.lighting.filters.api.EffectFilterApi
import io.cyborgsquirrel.lighting.filters.requests.CreateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.requests.UpdateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.service.EffectFilterApiService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/filter")
class EffectFilterController(private val filterApiService: EffectFilterApiService) : EffectFilterApi {
    override fun getAllEffectFilters(): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun getFiltersForEffect(effectUuid: String): HttpResponse<Any> {
        return try {
            val filters = filterApiService.getFiltersForEffect(effectUuid)
            HttpResponse.ok(filters)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun getEffectFilter(uuid: String): HttpResponse<Any> {
        return try {
            val filter = filterApiService.getFilter(uuid)
            HttpResponse.ok(filter)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun createEffectFilter(request: CreateEffectFilterRequest): HttpResponse<Any> {
        return try {
            val uuid = filterApiService.createFilter(request)
            HttpResponse.created(uuid)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun updateEffectFilter(uuid: String, request: UpdateEffectFilterRequest): HttpResponse<Any> {
        return try {
            filterApiService.updateFilter(uuid, request)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun deleteEffectFilter(uuid: String): HttpResponse<Any> {
        return try {
            filterApiService.deleteFilter(uuid)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }
}