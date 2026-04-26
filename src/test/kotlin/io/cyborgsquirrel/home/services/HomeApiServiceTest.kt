package io.cyborgsquirrel.home.services

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.home.responses.ActiveEffectResponse
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.EffectPaletteConstants
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.SpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
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

        response.clients shouldBe 0
        response.strips shouldBe 0
        response.effects shouldBe 0
        response.palettes shouldBe 0
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

        val playingUuid = UUID.randomUUID().toString()
        val pausedUuid = UUID.randomUUID().toString()
        val idleUuid = UUID.randomUUID().toString()
        val stripModel = SingleLedStripModel(
            name = strip.name!!,
            uuid = strip.uuid!!,
            pin = strip.pin!!,
            length = strip.length!!,
            height = strip.height,
            blendMode = strip.blendMode!!,
            brightness = strip.brightness!!,
            clientUuid = client.uuid!!,
            inverted = false,
        )
        val spectrumSettings = SpectrumEffectSettings.default(60)

        activeLightEffectService.addOrUpdateEffect(
            ActiveLightEffect(
                effectUuid = playingUuid,
                priority = 0,
                skipFramesIfBlank = true,
                status = LightEffectStatus.Playing,
                effect = SpectrumLightEffect(60, spectrumSettings, null),
                filters = listOf(),
                strip = stripModel,
            )
        )
        activeLightEffectService.addOrUpdateEffect(
            ActiveLightEffect(
                effectUuid = pausedUuid,
                priority = 0,
                skipFramesIfBlank = true,
                status = LightEffectStatus.Paused,
                effect = SpectrumLightEffect(60, spectrumSettings, null),
                filters = listOf(),
                strip = stripModel,
            )
        )
        activeLightEffectService.addOrUpdateEffect(
            ActiveLightEffect(
                effectUuid = idleUuid,
                priority = 0,
                skipFramesIfBlank = true,
                status = LightEffectStatus.Idle,
                effect = SpectrumLightEffect(60, spectrumSettings, null),
                filters = listOf(),
                strip = stripModel,
            )
        )

        val response = service.getHome()

        response.activeEffects.size shouldBe 2
        response.activeEffects shouldContainExactly listOf(
            ActiveEffectResponse(uuid = playingUuid, status = LightEffectStatus.Playing, stripUuid = strip.uuid!!),
            ActiveEffectResponse(uuid = pausedUuid, status = LightEffectStatus.Paused, stripUuid = strip.uuid!!),
        )
    }
})
