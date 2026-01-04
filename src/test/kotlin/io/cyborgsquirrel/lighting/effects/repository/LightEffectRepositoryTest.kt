package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.led_strips.repository.H2PoolMemberLedStripRepository
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.test_helpers.normalizeNumberTypes
import io.cyborgsquirrel.test_helpers.objectToMap
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class LightEffectRepositoryTest(
    private val objectMapper: ObjectMapper,
    private val lightEffectRepository: H2LightEffectRepository,
    private val clientRepository: H2LedStripClientRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripPoolRepository: H2LedStripPoolRepository,
    private val poolMemberLedStripRepository: H2PoolMemberLedStripRepository,
) : StringSpec({

    val settings = NightriderEffectSettings.default()

    fun verifyLightEffectEntity(
        newEntity: LightEffectEntity,
        expectedEntity: LightEffectEntity,
    ) {
        newEntity.strip?.id shouldBe expectedEntity.strip?.id
        newEntity.pool?.id shouldBe expectedEntity.pool?.id
        newEntity.uuid shouldBe expectedEntity.uuid
        newEntity.name shouldBe expectedEntity.name
        newEntity.settings!!.map { normalizeNumberTypes(it.value) } shouldBe expectedEntity.settings!!.map {
            normalizeNumberTypes(
                it.value
            )
        }
    }

    afterTest {
        poolMemberLedStripRepository.deleteAll()
        lightEffectRepository.deleteAll()
        ledStripPoolRepository.deleteAll()
        ledStripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Create a light effect entity" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Living Room",
                address = "192.168.1.1",
                clientType = ClientType.Pi,
                colorOrder = ColorOrder.RGB,
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222
            )
        )
        val strip = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Strip A",
                pin = PiClientPin.D18.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val settingsJson = objectToMap(objectMapper, settings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = settingsJson,
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                name = "Super cool effect",
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
            )
        )

        val newEntities = lightEffectRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectEntity(newEntities.first(), lightEffect)
    }

    "Create a light effect entity for a pool" {
        val client = clientRepository.save(
            LedStripClientEntity(
                name = "Living Room",
                address = "192.168.1.1",
                clientType = ClientType.Pi,
                colorOrder = ColorOrder.RGB,
                uuid = UUID.randomUUID().toString(),
                apiPort = 1111,
                wsPort = 2222
            )
        )
        val strip = ledStripRepository.save(
            LedStripEntity(
                client = client,
                uuid = UUID.randomUUID().toString(),
                name = "Strip A",
                pin = PiClientPin.D10.pinName,
                length = 60,
                blendMode = BlendMode.Average,
                brightness = 25,
            )
        )
        val pool = ledStripPoolRepository.save(
            LedStripPoolEntity(
                name = "Living Room",
                uuid = UUID.randomUUID().toString(),
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )
        val poolMember = poolMemberLedStripRepository.save(
            PoolMemberLedStripEntity(
                pool = pool,
                strip = strip,
                inverted = false,
                poolIndex = 1
            )
        )

        val settingsJson = objectToMap(objectMapper, settings)
        val lightEffect = lightEffectRepository.save(
            LightEffectEntity(
                settings = settingsJson,
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                name = "My effect",
                pool = pool,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
            )
        )

        val newEntities = lightEffectRepository.queryAll()
        newEntities.size shouldBe 1
        verifyLightEffectEntity(newEntities.first(), lightEffect)
        newEntities.first().pool?.members?.first()?.id shouldBe poolMember.id
    }
})