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
class LedStripRepositoryTest(
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
) : StringSpec({

    var clientEntity: LedStripClientEntity? = null

    beforeTest {
        clientEntity = clientRepository.save(
            LedStripClientEntity(
                name = "Hallway client",
                address = "192.168.50.210",
                wsPort = 8888,
                apiPort = 7777
            )
        )
    }

    afterTest {
        groupMemberLedStripRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a led strip entity" {
        val ledStripEntity = ledStripRepository.save(
            LedStripEntity(
                client = clientEntity,
                name = "Kitchen lights A",
                uuid = UUID.randomUUID().toString(),
                length = 120
            )
        )
        val groupEntityOptional = ledStripRepository.findById(ledStripEntity.id)
        groupEntityOptional.isPresent shouldBe true
    }

    "Query a strip with a join" {
        val ledStripEntity = ledStripRepository.save(
            LedStripEntity(
                client = clientEntity,
                name = "Kitchen lights A",
                uuid = UUID.randomUUID().toString(),
                length = 120
            )
        )
        val groupEntity = ledStripGroupRepository.save(
            LedStripGroupEntity(
                name = "Kitchen Group",
                uuid = UUID.randomUUID().toString()
            )
        )
        var i = 0
        val groupMembers = listOf(ledStripEntity).map { entity ->
            GroupMemberLedStripEntity(group = groupEntity, strip = entity, inverted = false, groupIndex = i++)
        }
        val savedGroupMembers = groupMemberLedStripRepository.saveAll(groupMembers)

        val stripEntityOptional = ledStripRepository.findByUuid(ledStripEntity.uuid!!)

        stripEntityOptional.isPresent shouldBe true

        val newStripEntity = stripEntityOptional.get()
        newStripEntity.uuid shouldBe ledStripEntity.uuid
        newStripEntity.name shouldBe ledStripEntity.name
        newStripEntity.client!!.id shouldBe clientEntity!!.id
        newStripEntity.client!!.name shouldBe clientEntity!!.name
        newStripEntity.members.size shouldBe 1
        newStripEntity.members.first().id shouldBe savedGroupMembers.first().id
        newStripEntity.members.first().inverted shouldBe savedGroupMembers.first().inverted
        newStripEntity.members.first().groupIndex shouldBe savedGroupMembers.first().groupIndex
    }
})