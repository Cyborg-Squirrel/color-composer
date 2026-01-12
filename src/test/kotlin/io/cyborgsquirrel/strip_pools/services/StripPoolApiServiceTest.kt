package io.cyborgsquirrel.strip_pools.services

import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class StripPoolApiServiceTest(
    private val stripPoolApiService: StripPoolApiService,
    private val poolRepository: H2LedStripPoolRepository,
    private val poolMemberRepository: H2PoolMemberLedStripRepository
) : StringSpec({

    afterTest {
        poolMemberRepository.deleteAll()
        poolRepository.deleteAll()
    }

    "getStripPool should throw exception for non-existent pool" {
        shouldThrow<ClientRequestException> {
            stripPoolApiService.getStripPool("non-existent-uuid")
        }
    }

    "getStripPool should return pool by uuid" {
        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "test-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        val result = stripPoolApiService.getStripPool("test-uuid")
        result.uuid shouldBe "test-uuid"
        result.name shouldBe "Test Pool"
        result.poolType shouldBe PoolType.Sync
        result.blendMode shouldBe BlendMode.Additive
        result.members.isEmpty() shouldBe true
    }

    "getStripPools should return empty list when no pools exist" {
        val result = stripPoolApiService.getStripPools()
        result.pools.isEmpty() shouldBe true
    }

    "getStripPools should return all pools" {
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

        val result = stripPoolApiService.getStripPools()
        result.pools.size shouldBe 2
    }

    "createStripPool should create new pool with valid uuid" {
        val request = CreateStripPoolRequest(
            name = "New Pool",
            poolType = PoolType.Unified,
            blendMode = BlendMode.Layer
        )

        val uuid = stripPoolApiService.createStripPool(request)
        uuid shouldNotBe null
        uuid.isEmpty() shouldBe false

        val savedPool = poolRepository.findByUuid(uuid)
        savedPool.isPresent shouldBe true
        savedPool.get().name shouldBe request.name
        savedPool.get().poolType shouldBe request.poolType
        savedPool.get().blendMode shouldBe request.blendMode
    }

    "createStripPool should create pools with different blend modes" {
        val blendModes = listOf(
            BlendMode.Additive,
            BlendMode.Average,
            BlendMode.Layer,
            BlendMode.UseHighest
        )

        blendModes.forEach { mode ->
            val request = CreateStripPoolRequest(
                name = "Pool with $mode",
                poolType = PoolType.Sync,
                blendMode = mode
            )
            val uuid = stripPoolApiService.createStripPool(request)
            val savedPool = poolRepository.findByUuid(uuid)
            savedPool.get().blendMode shouldBe mode
        }
    }

    "updateStripPool should throw exception for non-existent pool" {
        val request = UpdateStripPoolRequest(
            name = "Updated Name",
            poolType = null,
            blendMode = null
        )

        shouldThrow<ClientRequestException> {
            stripPoolApiService.updateStripPool("non-existent", request)
        }
    }

    "updateStripPool should update pool name" {
        val pool = LedStripPoolEntity(
            name = "Original Name",
            uuid = "pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        val request = UpdateStripPoolRequest(
            name = "Updated Name",
            poolType = PoolType.Unified,
            blendMode = BlendMode.UseHighest
        )

        stripPoolApiService.updateStripPool(pool.uuid!!, request)

        val updatedPool = poolRepository.findByUuid(pool.uuid!!)
        updatedPool.get().name shouldBe request.name
        updatedPool.get().poolType shouldBe request.poolType
        updatedPool.get().blendMode shouldBe request.blendMode
    }

    "deletePool should delete existing pool" {
        val pool = LedStripPoolEntity(
            name = "Pool to Delete",
            uuid = "pool-to-delete",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        stripPoolApiService.deletePool(pool.uuid!!)

        val deletedPool = poolRepository.findByUuid(pool.uuid!!)
        deletedPool.isEmpty shouldBe true
    }

    "deletePool should not throw exception for non-existent pool" {
        // Should not throw any exception
        stripPoolApiService.deletePool("non-existent-pool")
    }

})
