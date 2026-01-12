package io.cyborgsquirrel.strip_pools.services

import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.repository.H2PoolMemberLedStripRepository
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolMembersRequest
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

        poolRepository.update(poolEntity)
    }

    fun updatePoolMembers(uuid: String, request: UpdateStripPoolMembersRequest) {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            throw ClientRequestException("Could not find strip pool with uuid $uuid")
        }

        val poolEntity = poolEntityOptional.get()
        val currentMembers = poolMemberRepository.findByPool(poolEntity)

        val requestedMembersMap = request.members.associateBy { it.stripUuid }

        currentMembers.forEach { currentMember ->
            val stripUuid = currentMember.strip?.uuid
            if (stripUuid != null && !requestedMembersMap.containsKey(stripUuid)) {
                poolMemberRepository.delete(currentMember)
            }
        }

        request.members.forEach { requestMember ->
            val stripOptional = stripRepository.findByUuid(requestMember.stripUuid)
            if (stripOptional.isEmpty) {
                throw ClientRequestException("Could not find strip with uuid ${requestMember.stripUuid}")
            }

            val stripEntity = stripOptional.get()
            val existingMember = currentMembers.firstOrNull { it.strip?.uuid == requestMember.stripUuid }

            if (existingMember != null) {
                existingMember.uuid = requestMember.uuid ?: existingMember.uuid
                existingMember.inverted = requestMember.inverted
                existingMember.poolIndex = requestMember.poolIndex
                poolMemberRepository.update(existingMember)
            } else {
                val newMember = PoolMemberLedStripEntity(
                    pool = poolEntity,
                    strip = stripEntity,
                    uuid = requestMember.uuid ?: UUID.randomUUID().toString(),
                    inverted = requestMember.inverted,
                    poolIndex = requestMember.poolIndex
                )
                poolMemberRepository.save(newMember)
            }
        }
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