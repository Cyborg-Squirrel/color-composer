package io.cyborgsquirrel.setup.service

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.setup.responses.SetupStatus
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrips
import io.cyborgsquirrel.test_helpers.saveLightEffect
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class SetupServiceTest(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val setupStatusCheckService: SetupStatusCheckService,
    private val objectMapper: ObjectMapper,
) : StringSpec({

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
        createLedStripClientEntity(clientRepository, "Bedroom", "192.168.50.210", 8888, 7777)
        val setupStatus = setupStatusCheckService.getSetupStatus()
        setupStatus shouldBe SetupStatus.NoStrips
    }

    "No effects setup - one client and one strip setup" {
        val client = createLedStripClientEntity(clientRepository, "Bedroom", "192.168.50.210", 8888, 7777)
        saveLedStrips(stripRepository, client, listOf("Bedroom lights" to 120))
        val setupStatus = setupStatusCheckService.getSetupStatus()
        setupStatus shouldBe SetupStatus.NoEffects
    }

    "Everything setup" {
        val client = createLedStripClientEntity(clientRepository, "Bedroom", "192.168.50.210", 8888, 7777)
        val strips = saveLedStrips(stripRepository, client, listOf("Bedroom lights" to 120))
        saveLightEffect(effectRepository, objectMapper, strips.first())
        val setupStatus = setupStatusCheckService.getSetupStatus()
        setupStatus shouldBe SetupStatus.SetupComplete
    }
})