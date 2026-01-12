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
class LedStripRepositoryTest(
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripPoolRepository: H2LedStripPoolRepository,
    private val poolMemberLedStripRepository: H2PoolMemberLedStripRepository,
) : StringSpec({

    var clientEntity: LedStripClientEntity? = null

    fun createClientEntity(): LedStripClientEntity = clientRepository.save(
        LedStripClientEntity(
            name = "Hallway client",
            address = "192.168.50.210",
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.RGB,
            uuid = UUID.randomUUID().toString(),
            wsPort = 8888,
            apiPort = 7777
        )
    )

    fun createLedStripEntity(name: String, uuid: String, length: Int, brightness: Int): LedStripEntity {
        return ledStripRepository.save(
            LedStripEntity(
                client = clientEntity,
                name = name,
                pin = PiClientPin.D12.pinName,
                uuid = uuid,
                length = length,
                blendMode = BlendMode.Average,
                brightness = brightness,
            )
        )
    }

    fun createLedStripPoolEntity(name: String, uuid: String): LedStripPoolEntity {
        return ledStripPoolRepository.save(
            LedStripPoolEntity(
                name = name,
                uuid = uuid,
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )
    }

    fun createPoolMembers(
        poolEntity: LedStripPoolEntity,
        ledStrips: List<LedStripEntity>
    ): List<PoolMemberLedStripEntity> {
        return ledStrips.mapIndexed { index, ledStrip ->
            PoolMemberLedStripEntity(
                pool = poolEntity,
                strip = ledStrip,
                uuid = UUID.randomUUID().toString(),
                inverted = false,
                poolIndex = index
            )
        }
    }

    beforeTest {
        clientEntity = createClientEntity()
    }

    afterTest {
        poolMemberLedStripRepository.deleteAll()
        ledStripPoolRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a led strip entity" {
        val ledStripId = UUID.randomUUID().toString()
        val ledStripEntity =
            createLedStripEntity(name = "Kitchen lights A", uuid = ledStripId, length = 120, brightness = 50)

        val retrievedLedStripOptional = ledStripRepository.findById(ledStripEntity.id)
        retrievedLedStripOptional.isPresent shouldBe true
    }

    "Query a strip with a join" {
        val ledStripUuid = UUID.randomUUID().toString()
        val ledStripEntity =
            createLedStripEntity(name = "Kitchen lights A", uuid = ledStripUuid, length = 120, brightness = 50)

        val poolUuid = UUID.randomUUID().toString()
        val poolEntity = createLedStripPoolEntity(name = "Kitchen pool", uuid = poolUuid)

        val poolMembers = createPoolMembers(poolEntity, listOf(ledStripEntity))
        val savedPoolMembers = poolMemberLedStripRepository.saveAll(poolMembers)

        val retrievedLedStripOptional = ledStripRepository.findByUuid(ledStripUuid)
        retrievedLedStripOptional.isPresent shouldBe true

        val retrievedLedStrip = retrievedLedStripOptional.get()
        with(retrievedLedStrip) {
            uuid shouldBe ledStripUuid
            name shouldBe ledStripEntity.name
            blendMode shouldBe BlendMode.Average
            client!!.id shouldBe clientEntity!!.id
            client!!.name shouldBe clientEntity!!.name
            members.size shouldBe 1
            members.first().apply {
                id shouldBe savedPoolMembers.first().id
                uuid shouldBe savedPoolMembers.first().uuid
                inverted shouldBe savedPoolMembers.first().inverted
                poolIndex shouldBe savedPoolMembers.first().poolIndex
            }
            brightness shouldBe ledStripEntity.brightness
        }
    }
})