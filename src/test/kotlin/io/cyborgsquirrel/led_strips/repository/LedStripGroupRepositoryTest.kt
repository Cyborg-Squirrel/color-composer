package io.cyborgsquirrel.led_strips.repository

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.GroupMemberLedStripEntity
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripGroupEntity
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.lighting.enums.BlendMode
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
        LedStripClientEntity(
            name = name,
            address = address,
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
            )
        )

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
        val stripA = saveLedStrip(client, "Hallway lights A", 120, PiClientPin.D10.pinName)
        val stripB = saveLedStrip(client, "Hallway lights B", 60, PiClientPin.D10.pinName)
        val savedStrips = listOf(stripA, stripB)
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

        val fetchedGroup = ledStripGroupRepository.queryById(group.id)
        fetchedGroup.isPresent shouldBe true

        val groupEntity = fetchedGroup.get()
        groupEntity.apply {
            uuid shouldBe group.uuid
            name shouldBe group.name
            members.size shouldBe 2
            members.first().matches(savedMembers.first())
            members.last().matches(savedMembers.last())
        }
    }
})
