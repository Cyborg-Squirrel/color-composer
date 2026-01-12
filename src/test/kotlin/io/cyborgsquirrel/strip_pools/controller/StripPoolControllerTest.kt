package io.cyborgsquirrel.strip_pools.controller

import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.strip_pools.api.StripPoolApi
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolResponse
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolsResponse
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class StripPoolControllerTest(
    @Client private val apiClient: StripPoolApi,
    private val poolRepository: H2LedStripPoolRepository,
    private val poolMemberRepository: H2PoolMemberLedStripRepository
) : StringSpec({

    afterTest {
        poolMemberRepository.deleteAll()
        poolRepository.deleteAll()
    }

    "getPools should return empty list when no pools exist" {
        val response = apiClient.getPools()
        response.status shouldBe HttpStatus.OK
        val body = response.body() as GetStripPoolsResponse
        body.pools.isEmpty() shouldBe true
    }

    "getPools should return list of pools" {
        val pool1 = LedStripPoolEntity(
            name = "Pool 1",
            uuid = UUID.randomUUID().toString(),
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        val pool2 = LedStripPoolEntity(
            name = "Pool 2",
            uuid = UUID.randomUUID().toString(),
            poolType = PoolType.Unified,
            blendMode = BlendMode.Average
        )
        poolRepository.save(pool1)
        poolRepository.save(pool2)

        val response = apiClient.getPools()
        response.status shouldBe HttpStatus.OK
        val body = response.body() as GetStripPoolsResponse
        body.pools.size shouldBe 2
        body.pools[0].name shouldBe "Pool 1"
        body.pools[1].name shouldBe "Pool 2"
    }

    "getPool should return bad request for non-existent pool" {
        val response = apiClient.getPool("non-existent-uuid")
        response.status shouldBe HttpStatus.BAD_REQUEST
    }

    "getPool should return pool by uuid" {
        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "test-pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Layer
        )
        poolRepository.save(pool)

        val response = apiClient.getPool("test-pool-uuid")
        response.status shouldBe HttpStatus.OK
        val body = response.body() as GetStripPoolResponse
        body.uuid shouldBe "test-pool-uuid"
        body.name shouldBe "Test Pool"
        body.poolType shouldBe PoolType.Sync
        body.blendMode shouldBe BlendMode.Layer
        body.members.isEmpty() shouldBe true
    }

    "createPool should create a new pool" {
        val request = CreateStripPoolRequest(
            name = "New Pool",
            poolType = PoolType.Unified,
            blendMode = BlendMode.UseHighest
        )

        val response = apiClient.createPool(request)
        response.status shouldBe HttpStatus.CREATED
        val uuid = response.body() as String
        uuid shouldNotBe null
        uuid.isEmpty() shouldBe false

        val savedPool = poolRepository.findByUuid(uuid)
        savedPool.isPresent shouldBe true
        savedPool.get().name shouldBe "New Pool"
        savedPool.get().poolType shouldBe PoolType.Unified
        savedPool.get().blendMode shouldBe BlendMode.UseHighest
    }

    "updatePool should update pool properties" {
        val pool = LedStripPoolEntity(
            name = "Original Name",
            uuid = "pool-to-update",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        val updateRequest = UpdateStripPoolRequest(
            name = "Updated Name",
            poolType = PoolType.Unified,
            blendMode = BlendMode.Average
        )

        val response = apiClient.updatePool("pool-to-update", updateRequest)
        response.status shouldBe HttpStatus.NO_CONTENT

        val updatedPool = poolRepository.findByUuid("pool-to-update")
        updatedPool.isPresent shouldBe true
        updatedPool.get().name shouldBe "Updated Name"
        updatedPool.get().poolType shouldBe PoolType.Unified
        updatedPool.get().blendMode shouldBe BlendMode.Average
    }

    "updatePool should return bad request for non-existent pool" {
        val updateRequest = UpdateStripPoolRequest(
            name = "Updated Name",
            poolType = null,
            blendMode = null
        )

        val response = apiClient.updatePool("non-existent", updateRequest)
        response.status shouldBe HttpStatus.BAD_REQUEST
    }

    "updatePool should handle partial updates" {
        val pool = LedStripPoolEntity(
            name = "Original Name",
            uuid = "pool-partial-update",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        val updateRequest = UpdateStripPoolRequest(
            name = "New Name",
            poolType = null,
            blendMode = null
        )

        val response = apiClient.updatePool("pool-partial-update", updateRequest)
        response.status shouldBe HttpStatus.NO_CONTENT

        val updatedPool = poolRepository.findByUuid("pool-partial-update")
        updatedPool.get().name shouldBe "New Name"
        updatedPool.get().poolType shouldBe PoolType.Sync
        updatedPool.get().blendMode shouldBe BlendMode.Additive
    }

    "deletePool should delete an existing pool" {
        val pool = LedStripPoolEntity(
            name = "Pool to Delete",
            uuid = "pool-to-delete",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        val response = apiClient.deletePool("pool-to-delete")
        response.status shouldBe HttpStatus.NO_CONTENT

        val deletedPool = poolRepository.findByUuid("pool-to-delete")
        deletedPool.isEmpty shouldBe true
    }

    "deletePool should handle deleting non-existent pool gracefully" {
        val response = apiClient.deletePool("non-existent-pool")
        response.status shouldBe HttpStatus.NO_CONTENT
    }

})
