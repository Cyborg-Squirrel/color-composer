package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.GroupMemberLedStripEntity
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LedStripGroupEntity
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LedStripRepositoryTest(
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
) : StringSpec({

    var clientEntity: LedStripClientEntity? = null

    fun createClientEntity(): LedStripClientEntity = clientRepository.save(
        LedStripClientEntity(
            name = "Hallway client",
            address = "192.168.50.210",
            uuid = UUID.randomUUID().toString(),
            wsPort = 8888,
            apiPort = 7777
        )
    )

    fun createLedStripEntity(name: String, uuid: String, length: Int): LedStripEntity {
        return ledStripRepository.save(
            LedStripEntity(
                client = clientEntity,
                name = name,
                uuid = uuid,
                length = length,
                blendMode = BlendMode.Average,
            )
        )
    }

    fun createLedStripGroupEntity(name: String, uuid: String): LedStripGroupEntity {
        return ledStripGroupRepository.save(
            LedStripGroupEntity(
                name = name,
                uuid = uuid
            )
        )
    }

    fun createGroupMembers(
        groupEntity: LedStripGroupEntity,
        ledStrips: List<LedStripEntity>
    ): List<GroupMemberLedStripEntity> {
        return ledStrips.mapIndexed { index, ledStrip ->
            GroupMemberLedStripEntity(
                group = groupEntity,
                strip = ledStrip,
                inverted = false,
                groupIndex = index
            )
        }
    }

    beforeTest {
        clientEntity = createClientEntity()
    }

    afterTest {
        groupMemberLedStripRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a led strip entity" {
        val ledStripId = UUID.randomUUID().toString()
        val ledStripEntity = createLedStripEntity(name = "Kitchen lights A", uuid = ledStripId, length = 120)

        val retrievedLedStripOptional = ledStripRepository.findById(ledStripEntity.id)
        retrievedLedStripOptional.isPresent shouldBe true
    }

    "Query a strip with a join" {
        val ledStripUuid = UUID.randomUUID().toString()
        val ledStripEntity = createLedStripEntity(name = "Kitchen lights A", uuid = ledStripUuid, length = 120)

        val groupUuid = UUID.randomUUID().toString()
        val groupEntity = createLedStripGroupEntity(name = "Kitchen Group", uuid = groupUuid)

        val groupMembers = createGroupMembers(groupEntity, listOf(ledStripEntity))
        val savedGroupMembers = groupMemberLedStripRepository.saveAll(groupMembers)

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
                id shouldBe savedGroupMembers.first().id
                inverted shouldBe savedGroupMembers.first().inverted
                groupIndex shouldBe savedGroupMembers.first().groupIndex
            }
        }
    }
})