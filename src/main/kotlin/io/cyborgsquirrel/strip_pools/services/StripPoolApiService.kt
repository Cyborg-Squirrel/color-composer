package io.cyborgsquirrel.strip_pools.services

import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.repository.H2PoolMemberLedStripRepository
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolResponse
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolsResponse
import io.cyborgsquirrel.strip_pools.responses.StripPoolMemberResponseModel
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class StripPoolApiService(
    private val poolRepository: H2LedStripPoolRepository,
    private val poolMemberRepository: H2PoolMemberLedStripRepository,
    private val stripRepository: H2LedStripRepository
) {

    private fun mapPoolEntityToModel(poolEntity: LedStripPoolEntity, memberEntities: List<PoolMemberLedStripEntity>): GetStripPoolResponse {
        val memberResponseModels = memberEntities.map {
            StripPoolMemberResponseModel(it.uuid!!, it.strip!!.uuid!!, it.inverted!!, it.poolIndex!!)
        }
        return GetStripPoolResponse(
            poolEntity.name!!,
            poolEntity.uuid!!,
            poolEntity.poolType!!,
            poolEntity.blendMode!!,
            memberResponseModels
        )
    }

    fun getStripPool(uuid: String): GetStripPoolResponse {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            throw ClientRequestException("Could not find pool with uuid $uuid")
        }

        val poolEntity = poolEntityOptional.get()
        val memberEntities = poolMemberRepository.findByPool(poolEntity)
        val response = mapPoolEntityToModel(poolEntity, memberEntities)
        return response
    }

    fun getStripPools(): GetStripPoolsResponse {
        val poolEntities = poolRepository.queryAll()
        val responseModels = poolEntities.map { mapPoolEntityToModel(it, it.members.toList()) }
        return GetStripPoolsResponse(responseModels)
    }

    fun createStripPool(request: CreateStripPoolRequest): String {
        val uuid = UUID.randomUUID().toString()
        val poolEntity =
            LedStripPoolEntity(
                name = request.name,
                blendMode = request.blendMode,
                poolType = request.poolType,
                uuid = uuid
            )
        poolRepository.save(poolEntity)
        return uuid
    }

    fun updateStripPool(uuid: String, request: UpdateStripPoolRequest) {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            throw ClientRequestException("Could not find strip pool with uuid $uuid")
        }

        val poolEntity = poolEntityOptional.get()
        if (request.name != null) {
            poolEntity.name = request.name
        }
        if (request.poolType != null) {
            poolEntity.poolType = request.poolType
        }
        if (request.blendMode != null) {
            poolEntity.blendMode = request.blendMode
        }

        poolRepository.save(poolEntity)
    }

    fun updatePoolMembers(uuid: String, request: UpdateStripPoolRequest) {
        // TODO
    }

    fun deletePool(uuid: String) {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            // Pool is already deleted
            return
        }

        poolRepository.delete(poolEntityOptional.get())
    }
}