package io.cyborgsquirrel.home.services

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.EffectPaletteConstants
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.test_helpers.saveLightEffect
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class HomeApiServiceTest(
    private val service: HomeApiService,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val paletteRepository: H2LightEffectPaletteRepository,
    private val activeLightEffectService: ActiveLightEffectService,
    private val objectMapper: ObjectMapper,
) : StringSpec({

    afterEach {
        activeLightEffectService.removeAllEffects()
        effectRepository.deleteAll()
        paletteRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Returns zero counts when database is empty" {
        val response = service.getHome()

        response.totalClients shouldBe 0
        response.totalStrips shouldBe 0
        response.totalEffects shouldBe 0
        response.totalPalettes shouldBe 0
        response.activeEffects shouldBe emptyList()
    }

    "Returns correct counts after inserting one of each entity" {
        val client = createLedStripClientEntity(clientRepository, "Client 1", "192.168.1.1", 8000, 8001)
        val strip = saveLedStrip(stripRepository, client, "Strip 1", 60, PiClientPin.D10.pinName, 100)
        saveLightEffect(effectRepository, objectMapper, strip)
        paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = mapOf(),
                name = "Palette 1",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val response = service.getHome()

        response.clients shouldBe 1
        response.strips shouldBe 1
        response.effects shouldBe 1
        response.palettes shouldBe 1
        response.activeEffects shouldBe emptyList()
    }

    "Returns correct counts with multiple entities of each type" {
        createLedStripClientEntity(clientRepository, "Client 1", "192.168.1.1", 8000, 8001)
        createLedStripClientEntity(clientRepository, "Client 2", "192.168.1.2", 8000, 8001)
        val client = createLedStripClientEntity(clientRepository, "Client 3", "192.168.1.3", 8000, 8001)

        val strip1 = saveLedStrip(stripRepository, client, "Strip 1", 60, PiClientPin.D10.pinName, 100)
        val strip2 = saveLedStrip(stripRepository, client, "Strip 2", 30, PiClientPin.D12.pinName, 100)

        saveLightEffect(effectRepository, objectMapper, strip1)
        saveLightEffect(effectRepository, objectMapper, strip2)

        paletteRepository.save(
            LightEffectPaletteEntity(uuid = UUID.randomUUID().toString(), settings = mapOf(), name = "Palette 1", type = EffectPaletteConstants.STATIC_COLOR_PALETTE)
        )
        paletteRepository.save(
            LightEffectPaletteEntity(uuid = UUID.randomUUID().toString(), settings = mapOf(), name = "Palette 2", type = EffectPaletteConstants.STATIC_COLOR_PALETTE)
        )
        paletteRepository.save(
            LightEffectPaletteEntity(uuid = UUID.randomUUID().toString(), settings = mapOf(), name = "Palette 3", type = EffectPaletteConstants.STATIC_COLOR_PALETTE)
        )

        val response = service.getHome()

        response.clients shouldBe 3
        response.strips shouldBe 2
        response.effects shouldBe 2
        response.palettes shouldBe 3
        response.activeEffects shouldBe emptyList()
    }

    "Returns active effects with Playing or Paused status" {
        val client = createLedStripClientEntity(clientRepository, "Client 1", "192.168.1.1", 8000, 8001)
        val strip = saveLedStrip(stripRepository, client, "Strip 1", 60, PiClientPin.D10.pinName, 100)

        val playingEffect = saveLightEffect(effectRepository, objectMapper, strip, LightEffectStatus.Playing)
        val pausedEffect = saveLightEffect(effectRepository, objectMapper, strip, LightEffectStatus.Paused)
        saveLightEffect(effectRepository, objectMapper, strip, LightEffectStatus.Idle)

        val response = service.getHome()

        response.activeEffects.size shouldBe 2
        response.activeEffects shouldContainExactly listOf(
            GetStripEffectResponse(
                uuid = playingEffect.uuid!!,
                status = LightEffectStatus.Playing,
                stripUuid = strip.uuid!!,
                name = playingEffect.name!!,
                type = playingEffect.type!!,
                settings = playingEffect.settings!!,
                paletteUuid = null,
            ),
            GetStripEffectResponse(
                uuid = pausedEffect.uuid!!,
                status = LightEffectStatus.Paused,
                stripUuid = strip.uuid!!,
                name = pausedEffect.name!!,
                type = pausedEffect.type!!,
                settings = pausedEffect.settings!!,
                paletteUuid = null,
            ),
        )
    }
})
