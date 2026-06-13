package io.cyborgsquirrel.strip_pools.services

import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.clients.status.ClientStatusService
import io.cyborgsquirrel.event_source.model.StripPoolEvent
import io.cyborgsquirrel.event_source.model.delta.PoolDelta
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.repository.LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.led_strips.repository.PoolMemberLedStripRepository
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolMembersRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolResponse
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolsResponse
import io.cyborgsquirrel.strip_pools.responses.StripPoolMemberResponseModel
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import jakarta.inject.Singleton
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Singleton
class StripPoolApiService(
    private val poolRepository: LedStripPoolRepository,
    private val poolMemberRepository: PoolMemberLedStripRepository,
    private val stripRepository: LedStripRepository,
    private val sseEventEmitter: SseEventEmitter,
    private val clientStatusService: ClientStatusService,
) {

    /** One batched lookup of every member's strip (with its client) keyed by strip uuid. */
    private fun fetchStripsByUuid(memberEntities: List<PoolMemberLedStripEntity>): Map<String, LedStripEntity> {
        val stripUuids = memberEntities.mapNotNull { it.strip?.uuid }.distinct()
        if (stripUuids.isEmpty()) return emptyMap()
        return stripRepository.findByUuidIn(stripUuids).associateBy { it.uuid }
    }

    private fun mapPoolEntityToModel(
        poolEntity: LedStripPoolEntity,
        memberEntities: List<PoolMemberLedStripEntity>
    ): GetStripPoolResponse = mapPoolEntityToModel(poolEntity, memberEntities, fetchStripsByUuid(memberEntities))

    private fun mapPoolEntityToModel(
        poolEntity: LedStripPoolEntity,
        memberEntities: List<PoolMemberLedStripEntity>,
        stripsByUuid: Map<String, LedStripEntity>,
    ): GetStripPoolResponse {
        val memberResponseModels = mutableListOf<StripPoolMemberResponseModel>()
        var atLeastOnePoolMemberInUse = false
        memberEntities.forEach { me ->
            val clientEntity = me.strip?.let { stripsByUuid[it.uuid] }?.client
            val clientStatus = if (clientEntity == null) null else clientStatusService.getStatusForClient(clientEntity)
                .getOrNull()?.status
            val inUse = clientStatus == ClientStatus.Active
            atLeastOnePoolMemberInUse = atLeastOnePoolMemberInUse || inUse
            memberResponseModels.add(
                StripPoolMemberResponseModel(
                    me.uuid,
                    me.strip!!.uuid,
                    me.inverted,
                    me.poolIndex,
                    inUse
                )
            )
        }

        return GetStripPoolResponse(
            poolEntity.name,
            poolEntity.uuid,
            poolEntity.poolType,
            poolEntity.blendMode,
            memberResponseModels,
            atLeastOnePoolMemberInUse
        )
    }

    fun getStripPool(uuid: String): GetStripPoolResponse {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            throw ResourceNotFoundException("Could not find pool with uuid $uuid")
        }

        val poolEntity = poolEntityOptional.get()
        val memberEntities = poolMemberRepository.findByPool(poolEntity)
        val response = mapPoolEntityToModel(poolEntity, memberEntities)
        return response
    }

    fun getStripPools(): GetStripPoolsResponse {
        val poolEntities = poolRepository.queryAll()
        // Batch every pool's member strips into a single lookup instead of one query per pool.
        val stripsByUuid = fetchStripsByUuid(poolEntities.flatMap { it.members })
        val responseModels = poolEntities.map { mapPoolEntityToModel(it, it.members.toList(), stripsByUuid) }
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
        sseEventEmitter.emit(StripPoolEvent.StripPoolCreated(uuid, mapPoolEntityToModel(poolEntity, listOf())))
        return uuid
    }

    fun updateStripPool(uuid: String, request: UpdateStripPoolRequest) {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            throw ResourceNotFoundException("Could not find strip pool with uuid $uuid")
        }

        val poolEntity = poolEntityOptional.get()
        val oldName = poolEntity.name
        val oldPoolType = poolEntity.poolType
        val oldBlendMode = poolEntity.blendMode
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
        val delta = PoolDelta(
            name = poolEntity.name.takeIf { it != oldName },
            poolType = poolEntity.poolType.takeIf { it != oldPoolType },
            blendMode = poolEntity.blendMode.takeIf { it != oldBlendMode },
        )
        sseEventEmitter.emit(StripPoolEvent.StripPoolUpdated(uuid, delta))
    }

    fun updatePoolMembers(uuid: String, request: UpdateStripPoolMembersRequest) {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            throw ResourceNotFoundException("Could not find strip pool with uuid $uuid")
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
        val updatedMembers = mapPoolEntityToModel(poolEntity, poolMemberRepository.findByPool(poolEntity)).members
        sseEventEmitter.emit(StripPoolEvent.StripPoolUpdated(uuid, PoolDelta(members = updatedMembers)))
    }

    fun deletePool(uuid: String) {
        val poolEntityOptional = poolRepository.findByUuid(uuid)
        if (poolEntityOptional.isEmpty) {
            // Pool is already deleted
            return
        }

        poolRepository.delete(poolEntityOptional.get())
        sseEventEmitter.emit(StripPoolEvent.StripPoolDeleted(uuid))
    }
}