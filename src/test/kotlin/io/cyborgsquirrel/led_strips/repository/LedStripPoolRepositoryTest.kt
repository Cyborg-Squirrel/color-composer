package io.cyborgsquirrel.led_strips.repository

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LedStripPoolRepositoryTest(
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripPoolRepository: H2LedStripPoolRepository,
    private val poolMemberLedStripRepository: H2PoolMemberLedStripRepository,
) : StringSpec({
    lateinit var ledStripA: LedStripEntity
    lateinit var ledStripB: LedStripEntity

    fun createLedStripClientEntity(
        name: String,
        address: String,
        wsPort: Int,
        apiPort: Int
    ): LedStripClientEntity = clientRepository.save(
        LedStripClientEntity(
            name = name,
            address = address,
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.RGB,
            uuid = UUID.randomUUID().toString(),
            wsPort = wsPort,
            apiPort = apiPort
        )
    )

    fun saveLedStrip(client: LedStripClientEntity, name: String, length: Int, pin: String): LedStripEntity =
        ledStripRepository.save(
            LedStripEntity(
                client = client,
                name = name,
                uuid = UUID.randomUUID().toString(),
                pin = pin,
                length = length,
                blendMode = BlendMode.Average,
                brightness = 33,
            )
        )

    fun createLedStripPoolEntity(name: String): LedStripPoolEntity =
        ledStripPoolRepository.save(
            LedStripPoolEntity(
                name = name,
                uuid = UUID.randomUUID().toString(),
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )

    fun createPoolMember(
        pool: LedStripPoolEntity,
        strip: LedStripEntity,
        poolIndex: Int,
        inverted: Boolean
    ): PoolMemberLedStripEntity = PoolMemberLedStripEntity(
        pool = pool,
        strip = strip,
        inverted = inverted,
        poolIndex = poolIndex
    )

    fun PoolMemberLedStripEntity.matches(other: PoolMemberLedStripEntity) {
        id shouldBe other.id
        poolIndex shouldBe other.poolIndex
        inverted shouldBe other.inverted
    }

    beforeTest {
        val client = createLedStripClientEntity("Hallway client", "192.168.50.210", 8888, 7777)
        val stripA = saveLedStrip(client, "Hallway lights A", 120, PiClientPin.D10.pinName)
        val stripB = saveLedStrip(client, "Hallway lights B", 60, PiClientPin.D10.pinName)
        val savedStrips = listOf(stripA, stripB)
        ledStripA = savedStrips[0]
        ledStripB = savedStrips[1]
    }

    afterTest {
        poolMemberLedStripRepository.deleteAll()
        ledStripPoolRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a led strip pool entity" {
        val pool = createLedStripPoolEntity("Hallway Pool")
        val poolFetched = ledStripPoolRepository.findById(pool.id)
        poolFetched.isPresent shouldBe true
    }

    "Query a pool with a join" {
        val pool = createLedStripPoolEntity("Hallway Pool")
        val poolMembers = listOf(
            createPoolMember(pool, ledStripA, 0, false),
            createPoolMember(pool, ledStripB, 1, false)
        )
        val savedMembers = poolMemberLedStripRepository.saveAll(poolMembers)

        val fetchedPool = ledStripPoolRepository.queryById(pool.id)
        fetchedPool.isPresent shouldBe true

        val poolEntity = fetchedPool.get()
        poolEntity.apply {
            uuid shouldBe pool.uuid
            name shouldBe pool.name
            members.size shouldBe 2
            members.first().matches(savedMembers.first())
            members.last().matches(savedMembers.last())
            blendMode shouldBe pool.blendMode
        }
    }
})
