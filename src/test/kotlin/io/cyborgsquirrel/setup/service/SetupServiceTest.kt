package io.cyborgsquirrel.setup.service

import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.setup.responses.status.SetupStatus
import io.cyborgsquirrel.test_helpers.objectToMap
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class SetupServiceTest(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val setupStatusCheckService: SetupStatusCheckService,
    private val objectMapper: ObjectMapper,
) : StringSpec({

    fun createLedStripClientEntity(name: String, address: String, apiPort: Int, wsPort: Int): LedStripClientEntity =
        clientRepository.save(
            LedStripClientEntity(
                name = name,
                address = address,
                uuid = UUID.randomUUID().toString(),
                apiPort = apiPort,
                wsPort = wsPort
            )
        )

    fun saveLedStrips(client: LedStripClientEntity, strips: List<Pair<String, Int>>): List<LedStripEntity> =
        strips.map { (name, length) ->
            stripRepository.save(
                LedStripEntity(
                    client = client,
                    name = name,
                    uuid = UUID.randomUUID().toString(),
                    length = length,
                    blendMode = BlendMode.Average,
                )
            )
        }

    fun saveLightEffect(strip: LedStripEntity): LightEffectEntity =
        effectRepository.save(
            LightEffectEntity(
                strip = strip,
                uuid = UUID.randomUUID().toString(),
                settings = objectToMap(objectMapper, SpectrumEffectSettings(strip.length!!, animated = false)),
                name = LightEffectConstants.SPECTRUM_NAME,
                status = LightEffectStatus.Created
            )
        )

    afterTest {
        effectRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Nothing setup" {
        val setupStatus = setupStatusCheckService.getSetupStatus()
        setupStatus shouldBe SetupStatus.NoClients
    }

    "No strips setup - one client setup" {
        val client = createLedStripClientEntity("Bedroom", "192.168.50.210", 8888, 7777)
        val setupStatus = setupStatusCheckService.getSetupStatus()
        setupStatus shouldBe SetupStatus.NoStrips
    }

    "No effects setup - one client and one strip setup" {
        val client = createLedStripClientEntity("Bedroom", "192.168.50.210", 8888, 7777)
        val strips = saveLedStrips(client, listOf("Bedroom lights" to 120))
        val setupStatus = setupStatusCheckService.getSetupStatus()
        setupStatus shouldBe SetupStatus.NoEffects
    }

    "Everything setup" {
        val client = createLedStripClientEntity("Bedroom", "192.168.50.210", 8888, 7777)
        val strips = saveLedStrips(client, listOf("Bedroom lights" to 120))
        val effect = saveLightEffect(strips.first())
        val setupStatus = setupStatusCheckService.getSetupStatus()
        setupStatus shouldBe SetupStatus.SetupComplete
    }
})