package io.cyborgsquirrel.lighting.effects.api

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.EffectPaletteConstants
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_palette.settings.StaticPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.SettingsPalette
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.objectToMap
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class EffectApiConsolidationTest(
    @Client private val apiClient: EffectApi,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val paletteRepository: H2LightEffectPaletteRepository,
    private val objectMapper: ObjectMapper
) : StringSpec({

    afterEach {
        paletteRepository.deleteAll()
        effectRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "getEffects endpoint - bad request when both stripUuid and poolUuid provided" {
        val client = createLedStripClientEntity(clientRepository, "Test", "192.168.1.1", 1000, 1001)
        val strip = saveLedStrip(stripRepository, client, "Strip", 100, PiClientPin.D21.pinName, 50)

        val response = apiClient.getEffects(strip.uuid, UUID.randomUUID().toString())
        response.status shouldBe HttpStatus.BAD_REQUEST
    }

    "getEffects endpoint - returns all effects without filters" {
        val client = createLedStripClientEntity(clientRepository, "Test", "192.168.1.1", 1000, 1001)
        val strip1 = saveLedStrip(stripRepository, client, "Strip1", 100, PiClientPin.D10.pinName, 50)
        val strip2 = saveLedStrip(stripRepository, client, "Strip2", 100, PiClientPin.D21.pinName, 50)

        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(
                    primaryColor = RgbColor.Blue,
                    secondaryColor = RgbColor.Blue,
                    tertiaryColor = null,
                    otherColors = listOf()
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Blue",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val settings = objectToMap(objectMapper, NightriderEffectSettings.default())

        effectRepository.save(
            LightEffectEntity(
                strip = strip1,
                palette = palette,
                name = "Effect 1",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = settings
            )
        )

        effectRepository.save(
            LightEffectEntity(
                strip = strip2,
                palette = palette,
                name = "Effect 2",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = settings
            )
        )

        val response = apiClient.getEffects(null, null)
        response.status shouldBe HttpStatus.OK
        val body = response.body() as GetEffectsResponse
        body.effects.size shouldBe 2
    }

    "getEffects endpoint - filters correctly by strip UUID" {
        val client = createLedStripClientEntity(clientRepository, "Test", "192.168.1.1", 1000, 1001)
        val strip1 = saveLedStrip(stripRepository, client, "Strip1", 100, PiClientPin.D10.pinName, 50)
        val strip2 = saveLedStrip(stripRepository, client, "Strip2", 100, PiClientPin.D21.pinName, 50)

        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(
                    primaryColor = RgbColor.Red,
                    secondaryColor = RgbColor.Red,
                    tertiaryColor = null,
                    otherColors = listOf()
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Red",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val settings = objectToMap(objectMapper, NightriderEffectSettings.default())

        effectRepository.save(
            LightEffectEntity(
                strip = strip1,
                palette = palette,
                name = "Effect 1",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Playing,
                settings = settings
            )
        )

        effectRepository.save(
            LightEffectEntity(
                strip = strip2,
                palette = palette,
                name = "Effect 2",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = settings
            )
        )

        // Query for strip1
        val response1 = apiClient.getEffects(strip1.uuid, null)
        response1.status shouldBe HttpStatus.OK
        val body1 = response1.body() as GetEffectsResponse
        body1.effects.size shouldBe 1
        body1.effects[0].name shouldBe "Effect 1"

        // Query for strip2
        val response2 = apiClient.getEffects(strip2.uuid, null)
        response2.status shouldBe HttpStatus.OK
        val body2 = response2.body() as GetEffectsResponse
        body2.effects.size shouldBe 1
        body2.effects[0].name shouldBe "Effect 2"
    }

    "getEffects endpoint - handles empty results gracefully" {
        val response = apiClient.getEffects(null, null)
        response.status shouldBe HttpStatus.OK
        val body = response.body() as GetEffectsResponse
        body.effects.isEmpty() shouldBe true
    }
})
