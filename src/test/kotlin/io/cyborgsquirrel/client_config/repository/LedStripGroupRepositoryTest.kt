package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.GroupMemberLedStripEntity
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LedStripGroupEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LedStripGroupRepositoryTest(
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
) : StringSpec({

    lateinit var ledStripEntityA: LedStripEntity
    lateinit var ledStripEntityB: LedStripEntity

    beforeTest {
        val ledStripClientEntity =
            clientRepository.save(
                LedStripClientEntity(
                    name = "Hallway client",
                    address = "192.168.50.210",
                    wsPort = 8888,
                    apiPort = 7777
                )
            )
        ledStripEntityA = ledStripRepository.save(
            LedStripEntity(
                client = ledStripClientEntity,
                name = "Hallway lights A",
                uuid = UUID.randomUUID().toString(),
                length = 120
            )
        )
        ledStripEntityB = ledStripRepository.save(
            LedStripEntity(
                client = ledStripClientEntity,
                name = "Hallway lights B",
                uuid = UUID.randomUUID().toString(),
                length = 60
            )
        )
    }

    afterTest {
        groupMemberLedStripRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a led strip group entity" {
        val groupEntity = ledStripGroupRepository.save(
            LedStripGroupEntity(
                name = "Hallway Group",
                uuid = UUID.randomUUID().toString()
            )
        )
        var i = 0
        val groupMembers = listOf(ledStripEntityA, ledStripEntityB).map { entity ->
            GroupMemberLedStripEntity(group = groupEntity, strip = entity, inverted = false, groupIndex = i++)
        }
        val savedGroupMembers = groupMemberLedStripRepository.saveAll(groupMembers)

        val groupEntityOptional = ledStripGroupRepository.findByName(groupEntity.name!!)

        groupEntityOptional.isPresent shouldBe true

        val newGroupEntity = groupEntityOptional.get()
        newGroupEntity.uuid shouldBe groupEntity.uuid
        newGroupEntity.name shouldBe groupEntity.name
        newGroupEntity.strips.size shouldBe 2
        newGroupEntity.strips.first().id shouldBe savedGroupMembers.first().id
        newGroupEntity.strips.first().inverted shouldBe savedGroupMembers.first().inverted
        newGroupEntity.strips.first().groupIndex shouldBe savedGroupMembers.first().groupIndex
        newGroupEntity.strips.last().id shouldBe savedGroupMembers.last().id
        newGroupEntity.strips.last().inverted shouldBe savedGroupMembers.last().inverted
        newGroupEntity.strips.last().groupIndex shouldBe savedGroupMembers.last().groupIndex
    }
})