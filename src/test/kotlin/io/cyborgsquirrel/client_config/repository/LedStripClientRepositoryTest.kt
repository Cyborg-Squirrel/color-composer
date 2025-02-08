package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LedStripClientRepositoryTest(
    private val ledStripClientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository
) : StringSpec({

    val demoClientEntity =
        LedStripClientEntity(name = "Living Room", address = "192.168.50.200", wsPort = 8888, apiPort = 80)
    val demoLedStripEntity =
        LedStripEntity(
            name = "Living Room Vertical Strip A",
            client = demoClientEntity,
            length = 60,
            uuid = UUID.randomUUID().toString()
        )

    fun verifyClientsAreEqual(retrievedEntity: LedStripClientEntity) {
        retrievedEntity.name shouldBe demoClientEntity.name
        retrievedEntity.address shouldBe demoClientEntity.address
        retrievedEntity.wsPort shouldBe demoClientEntity.wsPort
        retrievedEntity.apiPort shouldBe demoClientEntity.apiPort
    }

    afterTest {
        ledStripRepository.deleteAll()
        ledStripClientRepository.deleteAll()
    }

    "Create a client entity" {
        val savedEntity = ledStripClientRepository.save(demoClientEntity)
        // Default id value is -1 because it is not valid in SQL
        // If this saved successfully we should have an auto-generated id greater than 0
        savedEntity.id shouldBeGreaterThan 0
    }

    "Query a client entity by name" {
        ledStripClientRepository.save(demoClientEntity)
        val retrievedEntityOptional = ledStripClientRepository.findByName(demoClientEntity.name!!)

        retrievedEntityOptional.isPresent shouldBe true

        val retrievedEntity = retrievedEntityOptional.get()
        verifyClientsAreEqual(retrievedEntity)
    }

    "Query a client entity find all with join" {
        ledStripClientRepository.save(demoClientEntity)
        ledStripRepository.save(demoLedStripEntity)
        val retrievedEntities = ledStripClientRepository.findAllByIdGreaterThan()

        assert(retrievedEntities.isNotEmpty())
        retrievedEntities.size shouldBe 1

        val retrievedEntity = retrievedEntities.first()
        verifyClientsAreEqual(retrievedEntity)
        retrievedEntity.strips.size shouldBe 1
    }

    "Query a client entity with associated led strips" {
        ledStripClientRepository.save(demoClientEntity)
        ledStripRepository.save(demoLedStripEntity)
        val retrievedEntityOptional = ledStripClientRepository.findByName(demoClientEntity.name!!)

        retrievedEntityOptional.isPresent shouldBe true

        val retrievedEntity = retrievedEntityOptional.get()
        verifyClientsAreEqual(retrievedEntity)
        retrievedEntity.strips.size shouldBe 1
        retrievedEntity.strips.first().name shouldBe demoLedStripEntity.name
        retrievedEntity.strips.first().uuid shouldBe demoLedStripEntity.uuid
        retrievedEntity.strips.first().length shouldBe demoLedStripEntity.length
    }
})