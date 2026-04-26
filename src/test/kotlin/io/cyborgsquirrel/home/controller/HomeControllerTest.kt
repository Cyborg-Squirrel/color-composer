package io.cyborgsquirrel.home.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.home.api.HomeApi
import io.cyborgsquirrel.home.responses.HomeResponse
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
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class HomeControllerTest(
    @Client private val apiClient: HomeApi,
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

    "GET /home returns 200 with zero counts when database is empty" {
        val response = apiClient.getHome()

        response.status shouldBe HttpStatus.OK
        val body = response.body() as HomeResponse
        body.clients shouldBe 0
        body.strips shouldBe 0
        body.effects shouldBe 0
        body.palettes shouldBe 0
        body.activeEffects shouldBe emptyList()
    }

    "GET /home returns correct counts after inserting data" {
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

        val response = apiClient.getHome()

        response.status shouldBe HttpStatus.OK
        val body = response.body() as HomeResponse
        body.clients shouldBe 1
        body.strips shouldBe 1
        body.effects shouldBe 1
        body.palettes shouldBe 1
        body.activeEffects shouldBe emptyList()
    }

    "GET /home returns active effects list" {
        val client = createLedStripClientEntity(clientRepository, "Client 1", "192.168.1.1", 8000, 8001)
        val strip = saveLedStrip(stripRepository, client, "Strip 1", 60, PiClientPin.D10.pinName, 100)

        val playingEffect = saveLightEffect(effectRepository, objectMapper, strip, LightEffectStatus.Playing)

        val response = apiClient.getHome()

        response.status shouldBe HttpStatus.OK
        val body = response.body() as HomeResponse
        body.activeEffects shouldContainExactly listOf(
            GetStripEffectResponse(
                uuid = playingEffect.uuid!!,
                status = LightEffectStatus.Playing,
                stripUuid = strip.uuid!!,
                name = playingEffect.name!!,
                type = playingEffect.type!!,
                settings = playingEffect.settings!!,
                paletteUuid = null,
            ),
        )
    }
})
