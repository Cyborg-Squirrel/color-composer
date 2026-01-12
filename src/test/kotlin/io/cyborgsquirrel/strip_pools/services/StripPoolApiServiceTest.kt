package io.cyborgsquirrel.strip_pools.services

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.repository.H2PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.strip_pools.requests.CreateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.StripPoolMemberRequestModel
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolRequest
import io.cyborgsquirrel.strip_pools.requests.UpdateStripPoolMembersRequest
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
    private val poolMemberRepository: H2PoolMemberLedStripRepository,
    private val stripRepository: H2LedStripRepository,
    private val clientRepository: H2LedStripClientRepository
) : StringSpec({

    afterTest {
        poolMemberRepository.deleteAll()
        stripRepository.deleteAll()
        poolRepository.deleteAll()
        clientRepository.deleteAll()
    }

    fun createTestClient(): LedStripClientEntity {
        val client = LedStripClientEntity(
            name = "Test Client",
            uuid = UUID.randomUUID().toString(),
            address = "192.168.1.1",
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.RGB,
            wsPort = 8000,
            apiPort = 8001
        )
        clientRepository.save(client)
        return client
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

    "updatePoolMembers should throw exception for non-existent pool" {
        val request = UpdateStripPoolMembersRequest(members = emptyList())

        shouldThrow<ClientRequestException> {
            stripPoolApiService.updatePoolMembers("non-existent-pool", request)
        }
    }

    "updatePoolMembers should throw exception for non-existent strip" {
        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        val request = UpdateStripPoolMembersRequest(
            members = listOf(
                StripPoolMemberRequestModel(
                    uuid = "member-1",
                    stripUuid = "non-existent-strip",
                    inverted = false,
                    poolIndex = 0
                )
            )
        )

        shouldThrow<ClientRequestException> {
            stripPoolApiService.updatePoolMembers("pool-uuid", request)
        }
    }

    "updatePoolMembers should create new members" {
        val client = createTestClient()

        val strip1 = LedStripEntity(
            name = "Strip 1",
            uuid = "strip-1",
            pin = "D5",
            length = 30,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        val strip2 = LedStripEntity(
            name = "Strip 2",
            uuid = "strip-2",
            pin = "D6",
            length = 50,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        stripRepository.save(strip1)
        stripRepository.save(strip2)

        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        val request = UpdateStripPoolMembersRequest(
            members = listOf(
                StripPoolMemberRequestModel(
                    uuid = "member-1",
                    stripUuid = "strip-1",
                    inverted = false,
                    poolIndex = 0
                ),
                StripPoolMemberRequestModel(
                    uuid = "member-2",
                    stripUuid = "strip-2",
                    inverted = true,
                    poolIndex = 1
                )
            )
        )

        stripPoolApiService.updatePoolMembers("pool-uuid", request)

        val members = poolMemberRepository.findByPool(pool)
        members.size shouldBe 2
        members.find { it.uuid == "member-1" }?.inverted shouldBe false
        members.find { it.uuid == "member-2" }?.inverted shouldBe true
    }

    "updatePoolMembers should delete members not in request" {
        val client = createTestClient()

        val strip1 = LedStripEntity(
            name = "Strip 1",
            uuid = "strip-1",
            pin = "D5",
            length = 30,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        val strip2 = LedStripEntity(
            name = "Strip 2",
            uuid = "strip-2",
            pin = "D6",
            length = 50,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        stripRepository.save(strip1)
        stripRepository.save(strip2)

        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        // Create initial members
        val member1 = PoolMemberLedStripEntity(
            pool = pool,
            strip = strip1,
            uuid = "member-1",
            inverted = false,
            poolIndex = 0
        )
        val member2 = PoolMemberLedStripEntity(
            pool = pool,
            strip = strip2,
            uuid = "member-2",
            inverted = false,
            poolIndex = 1
        )
        poolMemberRepository.save(member1)
        poolMemberRepository.save(member2)

        // Update to only include strip-1
        val request = UpdateStripPoolMembersRequest(
            members = listOf(
                StripPoolMemberRequestModel(
                    uuid = "member-1",
                    stripUuid = "strip-1",
                    inverted = false,
                    poolIndex = 0
                )
            )
        )

        stripPoolApiService.updatePoolMembers("pool-uuid", request)

        val members = poolMemberRepository.findByPool(pool)
        members.size shouldBe 1
        members[0].uuid shouldBe "member-1"
    }

    "updatePoolMembers should update existing members" {
        val client = createTestClient()

        val strip1 = LedStripEntity(
            name = "Strip 1",
            uuid = "strip-1",
            pin = "D5",
            length = 30,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        stripRepository.save(strip1)

        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        // Create initial member
        val member = PoolMemberLedStripEntity(
            pool = pool,
            strip = strip1,
            uuid = "member-1",
            inverted = false,
            poolIndex = 0
        )
        poolMemberRepository.save(member)

        // Update the member
        val request = UpdateStripPoolMembersRequest(
            members = listOf(
                StripPoolMemberRequestModel(
                    uuid = "member-1",
                    stripUuid = "strip-1",
                    inverted = true,
                    poolIndex = 5
                )
            )
        )

        stripPoolApiService.updatePoolMembers("pool-uuid", request)

        val updatedMembers = poolMemberRepository.findByPool(pool)
        updatedMembers.size shouldBe 1
        updatedMembers[0].inverted shouldBe true
        updatedMembers[0].poolIndex shouldBe 5
    }

    "updatePoolMembers should handle subset and superset of members" {
        val client = createTestClient()

        val strip1 = LedStripEntity(
            name = "Strip 1",
            uuid = "strip-1",
            pin = "D5",
            length = 30,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        val strip2 = LedStripEntity(
            name = "Strip 2",
            uuid = "strip-2",
            pin = "D6",
            length = 50,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        val strip3 = LedStripEntity(
            name = "Strip 3",
            uuid = "strip-3",
            pin = "D7",
            length = 40,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        stripRepository.save(strip1)
        stripRepository.save(strip2)
        stripRepository.save(strip3)

        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        // Create initial members with strip1 and strip2
        val member1 = PoolMemberLedStripEntity(
            pool = pool,
            strip = strip1,
            uuid = "member-1",
            inverted = false,
            poolIndex = 0
        )
        val member2 = PoolMemberLedStripEntity(
            pool = pool,
            strip = strip2,
            uuid = "member-2",
            inverted = false,
            poolIndex = 1
        )
        poolMemberRepository.save(member1)
        poolMemberRepository.save(member2)

        // Update to include strip2 and strip3 (remove strip1, add strip3)
        val request = UpdateStripPoolMembersRequest(
            members = listOf(
                StripPoolMemberRequestModel(
                    uuid = "member-2",
                    stripUuid = "strip-2",
                    inverted = false,
                    poolIndex = 0
                ),
                StripPoolMemberRequestModel(
                    uuid = "member-3",
                    stripUuid = "strip-3",
                    inverted = true,
                    poolIndex = 1
                )
            )
        )

        stripPoolApiService.updatePoolMembers("pool-uuid", request)

        val members = poolMemberRepository.findByPool(pool)
        members.size shouldBe 2
        members.find { it.uuid == "member-1" } shouldBe null
        members.find { it.uuid == "member-2" } shouldNotBe null
        members.find { it.uuid == "member-3" } shouldNotBe null
    }

    "updatePoolMembers should clear all members when request is empty" {
        val client = createTestClient()

        val strip1 = LedStripEntity(
            name = "Strip 1",
            uuid = "strip-1",
            pin = "D5",
            length = 30,
            height = 1,
            blendMode = BlendMode.Additive,
            brightness = 100,
            client = client
        )
        stripRepository.save(strip1)

        val pool = LedStripPoolEntity(
            name = "Test Pool",
            uuid = "pool-uuid",
            poolType = PoolType.Sync,
            blendMode = BlendMode.Additive
        )
        poolRepository.save(pool)

        // Create initial member
        val member = PoolMemberLedStripEntity(
            pool = pool,
            strip = strip1,
            uuid = "member-1",
            inverted = false,
            poolIndex = 0
        )
        poolMemberRepository.save(member)

        // Update with empty members list
        val request = UpdateStripPoolMembersRequest(members = emptyList())

        stripPoolApiService.updatePoolMembers("pool-uuid", request)

        val members = poolMemberRepository.findByPool(pool)
        members.isEmpty() shouldBe true
    }

})
