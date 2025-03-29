package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LedStripClientRepositoryTest(
    private val ledStripClientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository
) : StringSpec({

    val demoClientEntity = LedStripClientEntity(
        name = "Living Room",
        address = "192.168.50.200",
        uuid = UUID.randomUUID().toString(),
        wsPort = 8888,
        apiPort = 80
    )

    val demoLedStripEntity = LedStripEntity(
        name = "Living Room Vertical Strip A",
        client = demoClientEntity,
        length = 60,
        uuid = UUID.randomUUID().toString(),
        blendMode = BlendMode.Layer,
    )

    fun assertClientsAreEqual(expected: LedStripClientEntity, actual: LedStripClientEntity) {
        actual.name shouldBe expected.name
        actual.address shouldBe expected.address
        actual.wsPort shouldBe expected.wsPort
        actual.apiPort shouldBe expected.apiPort
    }

    afterTest {
        ledStripRepository.deleteAll()
        ledStripClientRepository.deleteAll()
    }

    "should create a client entity with a valid auto-generated id" {
        val savedEntity = ledStripClientRepository.save(demoClientEntity)
        savedEntity.id shouldBeGreaterThan 0
    }

    "should query a client entity by name" {
        ledStripClientRepository.save(demoClientEntity)
        val retrievedEntity =
            ledStripClientRepository.findByUuid(demoClientEntity.uuid.orEmpty())
        retrievedEntity.isPresent shouldBe true
        assertClientsAreEqual(demoClientEntity, retrievedEntity.get())
    }

    "should find all client entities with ID greater than 0 and verify relations" {
        ledStripClientRepository.save(demoClientEntity)
        ledStripRepository.save(demoLedStripEntity)
        val retrievedEntities = ledStripClientRepository.queryAll()
        retrievedEntities.size shouldBe 1
        val retrievedEntity = retrievedEntities.first()
        assertClientsAreEqual(demoClientEntity, retrievedEntity)
        retrievedEntity.strips.size shouldBe 1
    }

    "should query a client entity with associated LED strips and verify data" {
        ledStripClientRepository.save(demoClientEntity)
        ledStripRepository.save(demoLedStripEntity)
        val retrievedEntity = ledStripClientRepository.findByUuid(demoClientEntity.uuid.orEmpty())
        retrievedEntity.isPresent shouldBe true
        retrievedEntity.get().apply {
            assertClientsAreEqual(demoClientEntity, this)
            strips.size shouldBe 1
            strips.first().let { strip ->
                strip.name shouldBe demoLedStripEntity.name
                strip.uuid shouldBe demoLedStripEntity.uuid
                strip.length shouldBe demoLedStripEntity.length
                strip.blendMode shouldBe BlendMode.Layer
            }
        }
    }
})