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
    lateinit var ledStripA: LedStripEntity
    lateinit var ledStripB: LedStripEntity

    fun createLedStripClientEntity(
        name: String,
        address: String,
        wsPort: Int,
        apiPort: Int
    ): LedStripClientEntity = clientRepository.save(
        LedStripClientEntity(name = name, address = address, wsPort = wsPort, apiPort = apiPort)
    )

    fun saveLedStrips(client: LedStripClientEntity, strips: List<Pair<String, Int>>): List<LedStripEntity> =
        strips.map { (name, length) ->
            ledStripRepository.save(
                LedStripEntity(
                    client = client,
                    name = name,
                    uuid = UUID.randomUUID().toString(),
                    length = length
                )
            )
        }

    fun createLedStripGroupEntity(name: String): LedStripGroupEntity =
        ledStripGroupRepository.save(
            LedStripGroupEntity(
                name = name,
                uuid = UUID.randomUUID().toString()
            )
        )

    fun createGroupMember(
        group: LedStripGroupEntity,
        strip: LedStripEntity,
        groupIndex: Int,
        inverted: Boolean
    ): GroupMemberLedStripEntity = GroupMemberLedStripEntity(
        group = group,
        strip = strip,
        inverted = inverted,
        groupIndex = groupIndex
    )

    fun GroupMemberLedStripEntity.matches(other: GroupMemberLedStripEntity) {
        id shouldBe other.id
        groupIndex shouldBe other.groupIndex
        inverted shouldBe other.inverted
    }

    beforeTest {
        val client = createLedStripClientEntity("Hallway client", "192.168.50.210", 8888, 7777)
        val savedStrips = saveLedStrips(client, listOf("Hallway lights A" to 120, "Hallway lights B" to 60))
        ledStripA = savedStrips[0]
        ledStripB = savedStrips[1]
    }

    afterTest {
        groupMemberLedStripRepository.deleteAll()
        ledStripGroupRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a led strip group entity" {
        val group = createLedStripGroupEntity("Hallway Group")
        val groupFetched = ledStripGroupRepository.findById(group.id)
        groupFetched.isPresent shouldBe true
    }

    "Query a group with a join" {
        val group = createLedStripGroupEntity("Hallway Group")
        val groupMembers = listOf(
            createGroupMember(group, ledStripA, 0, false),
            createGroupMember(group, ledStripB, 1, false)
        )
        val savedMembers = groupMemberLedStripRepository.saveAll(groupMembers)

        val fetchedGroup = ledStripGroupRepository.findByName(group.name!!)
        fetchedGroup.isPresent shouldBe true

        val groupEntity = fetchedGroup.get()
        groupEntity.apply {
            uuid shouldBe group.uuid
            name shouldBe group.name
            strips.size shouldBe 2
            strips.first().matches(savedMembers.first())
            strips.last().matches(savedMembers.last())
        }
    }
})
