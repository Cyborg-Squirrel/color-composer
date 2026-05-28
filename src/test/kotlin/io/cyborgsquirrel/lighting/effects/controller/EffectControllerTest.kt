package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.repository.LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.led_strips.repository.PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.EffectPaletteConstants
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effect_palette.repository.LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_palette.settings.*
import io.cyborgsquirrel.lighting.effect_settings.entity.LightEffectSettingsEntity
import io.cyborgsquirrel.lighting.effect_settings.repository.LightEffectSettingsRepository
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.api.EffectApi
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetPoolEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.normalizeNumberTypes
import io.cyborgsquirrel.test_helpers.objectToMap
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.util.*

@MicronautTest
class EffectControllerTest(
    @param:Client private val apiClient: EffectApi,
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val poolRepository: LedStripPoolRepository,
    private val poolMemberRepository: PoolMemberLedStripRepository,
    private val effectRepository: LightEffectRepository,
    private val settingsRepository: LightEffectSettingsRepository,
    private val paletteRepository: LightEffectPaletteRepository,
    private val objectMapper: ObjectMapper
) : StringSpec({

    afterEach {
        effectRepository.deleteAll()
        paletteRepository.deleteAll()
        settingsRepository.deleteAll()
        poolMemberRepository.deleteAll()
        poolRepository.deleteAll()
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Getting all effects" {
        val client = createLedStripClientEntity(clientRepository, "Porch lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 100)
        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(
                    primaryColor = RgbColor.Red,
                    secondaryColor = RgbColor.Orange,
                    tertiaryColor = null,
                    otherColors = listOf()
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Warm color palette",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val nrSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        var effectEntity = LightEffectEntity(
            strip = strip,
            palette = palette,
            name = "Super cool effect",
            effectSettings = nrSettingsEntity,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Inactive,
        )
        effectEntity = effectRepository.save(effectEntity)

        val getAllEffectsHttpResponse = apiClient.getEffects(null, null)
        getAllEffectsHttpResponse.status shouldBe HttpStatus.OK

        val getAllEffectsResponse = getAllEffectsHttpResponse.body() as GetEffectsResponse
        getAllEffectsResponse.effects.size shouldBe 1
        val effectFromApi = getAllEffectsResponse.effects.first()

        effectFromApi.name shouldBe effectEntity.name
        effectFromApi.uuid shouldBe effectEntity.uuid
        effectFromApi.status shouldBe effectEntity.status
        effectFromApi.settingsUuid shouldBe effectEntity.effectSettings!!.uuid
        effectFromApi::class.java shouldBe GetStripEffectResponse::class.java
        (effectFromApi as GetStripEffectResponse).stripUuid shouldBe strip.uuid
        effectFromApi.paletteUuid shouldBe palette.uuid
    }

    "Getting effects for a strip" {
        val client = createLedStripClientEntity(clientRepository, "Living Room lights", "192.168.50.50", 50, 51)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D10.pinName, 100)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 100, PiClientPin.D21.pinName, 75)
        val strips = listOf(stripA, stripB)
        val paletteSettings = objectToMap(
            objectMapper,
            GradientPaletteSettings(
                mapOf(
                    0 to SettingsPalette(
                        primaryColor = RgbColor.Blue,
                        secondaryColor = RgbColor.Blue,
                        tertiaryColor = null,
                        otherColors = listOf()
                    ),
                    50 to SettingsPalette(
                        primaryColor = RgbColor.Cyan,
                        secondaryColor = RgbColor.Cyan,
                        tertiaryColor = null,
                        otherColors = listOf()
                    ),
                    100 to SettingsPalette(
                        primaryColor = RgbColor.Green,
                        secondaryColor = RgbColor.Green,
                        tertiaryColor = null,
                        otherColors = listOf()
                    )
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Cool gradient palette",
                type = EffectPaletteConstants.GRADIENT_COLOR_PALETTE_NAME,
            )
        )
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val nrSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        var effectEntity = LightEffectEntity(
            strip = strips.last(),
            palette = palette,
            name = "Super cool effect",
            effectSettings = nrSettingsEntity,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Inactive,
        )
        effectEntity = effectRepository.save(effectEntity)

        var getAllEffectsHttpResponse = apiClient.getEffects(strips.first().uuid, null)
        getAllEffectsHttpResponse.status shouldBe HttpStatus.OK

        var getAllEffectsResponse = getAllEffectsHttpResponse.body() as GetEffectsResponse
        getAllEffectsResponse.effects.isEmpty() shouldBe true

        getAllEffectsHttpResponse = apiClient.getEffects(strips.last().uuid, null)
        getAllEffectsResponse = getAllEffectsHttpResponse.body() as GetEffectsResponse
        getAllEffectsResponse.effects.size shouldBe 1
        val effectFromApi = getAllEffectsResponse.effects.first()

        effectFromApi.name shouldBe effectEntity.name
        effectFromApi.uuid shouldBe effectEntity.uuid
        effectFromApi.status shouldBe effectEntity.status
        effectFromApi.settingsUuid shouldBe effectEntity.effectSettings!!.uuid
        effectFromApi::class.java shouldBe GetStripEffectResponse::class.java
        (effectFromApi as GetStripEffectResponse).stripUuid shouldBe effectEntity.strip?.uuid
        effectFromApi.paletteUuid shouldBe palette.uuid
    }

    "Create an effect" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val request = CreateEffectRequest(
            strip.uuid,
            null,
            LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
            "Rainbow Nightrider",
            defaultNrSettings,
            paletteUuid = null,
            settingsUuid = null,
        )

        val createEffectHttpResponse = apiClient.createEffect(request)
        createEffectHttpResponse.status shouldBe HttpStatus.CREATED
        val effectUuid = createEffectHttpResponse.body() as String

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        val effectEntity = effectEntities.first()
        effectEntity.strip?.uuid shouldBe request.stripUuid
        effectEntity.name shouldBe request.name
        effectEntity.uuid shouldBe effectUuid
        effectEntity.effectSettings!!.settings.map { normalizeNumberTypes(it.value) } shouldBe request.settings!!.map {
            normalizeNumberTypes(
                it.value
            )
        }
        effectEntity.palette shouldBe null
    }

    "Updating an effect" {
        val client = createLedStripClientEntity(clientRepository, "Bedroom lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 66)
        val paletteSettings = objectToMap(
            objectMapper,
            TimeOfDayPaletteSettings(
                mapOf(
                    TimeOfDay.Midnight to SettingsPalette(
                        primaryColor = RgbColor.Purple,
                        secondaryColor = RgbColor.Purple,
                        tertiaryColor = null,
                        otherColors = listOf()
                    ),
                    TimeOfDay.Sunrise to SettingsPalette(
                        primaryColor = RgbColor.Yellow,
                        secondaryColor = RgbColor.Yellow,
                        tertiaryColor = null,
                        otherColors = listOf()
                    ),
                    TimeOfDay.Sunset to SettingsPalette(
                        primaryColor = RgbColor.Red,
                        secondaryColor = RgbColor.Red,
                        tertiaryColor = null,
                        otherColors = listOf()
                    )
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Time of day palette",
                type = EffectPaletteConstants.TIME_OF_DAY_COLOR_PALETTE,
            )
        )
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val updatedNrSettings = objectToMap(
            objectMapper,
            NightriderColorFillEffectSettings().copy(wrap = true)
        )
        val initialSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Initial NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        val updatedSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Updated NR Settings",
                settings = updatedNrSettings,
                isDefault = false,
            )
        )
        var effectEntity = LightEffectEntity(
            strip = strip,
            palette = palette,
            name = "Super cool light effect",
            effectSettings = initialSettingsEntity,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Inactive,
        )
        effectEntity = effectRepository.save(effectEntity)

        val updateRequest = UpdateEffectRequest(
            stripUuid = strip.uuid,
            poolUuid = null,
            paletteUuid = null,
            settingsUuid = updatedSettingsEntity.uuid,
            name = "New effect name",
        )
        val updateRequestHttpResponse = apiClient.updateEffect(effectEntity.uuid, updateRequest)
        updateRequestHttpResponse.status shouldBe HttpStatus.NO_CONTENT

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        effectEntities.first().strip?.uuid shouldBe strip.uuid
        effectEntities.first().name shouldBe updateRequest.name
        effectEntities.first().uuid shouldBe effectEntity.uuid
        effectEntities.first().effectSettings!!.settings.map { normalizeNumberTypes(it.value) } shouldBe updatedNrSettings.map {
            normalizeNumberTypes(
                it.value
            )
        }
        effectEntities.first().palette shouldBe palette
        // Update effect API doesn't support this - need to use updateEffectStatuses (/status) instead
        effectEntities.first().status shouldBe LightEffectStatus.Inactive
    }

    "Deleting an effect" {
        val client = createLedStripClientEntity(clientRepository, "Christmas Tree lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 50)
        val paletteSettings = objectToMap(
            objectMapper,
            ChangingStaticPaletteSettings(
                listOf(
                    SettingsPalette(
                        primaryColor = RgbColor.Red,
                        secondaryColor = RgbColor.Red,
                        tertiaryColor = null,
                        otherColors = listOf()
                    ), SettingsPalette(
                        primaryColor = RgbColor.Green,
                        secondaryColor = RgbColor.Green,
                        tertiaryColor = null,
                        otherColors = listOf()
                    ), SettingsPalette(
                        primaryColor = RgbColor.Blue,
                        secondaryColor = RgbColor.Blue,
                        tertiaryColor = null,
                        otherColors = listOf()
                    )
                ),
                Duration.ofMinutes(15),
                Duration.ofSeconds(20)
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Changing palette",
                type = EffectPaletteConstants.CHANGING_COLOR_STATIC_PALETTE_NAME,
            )
        )
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val nrSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        var effectEntity = LightEffectEntity(
            strip = strip,
            palette = palette,
            name = "Super cool effect",
            effectSettings = nrSettingsEntity,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Inactive,
        )
        effectEntity = effectRepository.save(effectEntity)

        val createEffectHttpResponse = apiClient.deleteEffect(effectEntity.uuid)
        createEffectHttpResponse.status shouldBe HttpStatus.NO_CONTENT
        effectRepository.findAll().isEmpty() shouldBe true
        paletteRepository.findAll().isEmpty() shouldBe false
    }

    "Create an effect for a LED strip pool" {
        val client = createLedStripClientEntity(clientRepository, "Living Room lights", "192.168.50.50", 50, 51)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D10.pinName, 100)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 150, PiClientPin.D21.pinName, 80)

        // Create a LED strip pool and add members
        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Living Room Pool",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripA, pool = pool, uuid = UUID.randomUUID().toString(),poolIndex = 0, inverted = false)
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripB, pool = pool, uuid = UUID.randomUUID().toString(),poolIndex = 1, inverted = false)
        )

        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(
                    primaryColor = RgbColor.Green,
                    secondaryColor = RgbColor.Cyan,
                    tertiaryColor = null,
                    otherColors = listOf()
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Cool green palette",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val createEffectRequest = CreateEffectRequest(
            stripUuid = null,
            poolUuid = pool.uuid,
            effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
            name = "Pool Nightrider Effect",
            settings = defaultNrSettings,
            paletteUuid = palette.uuid,
            settingsUuid = null,
        )
        val createEffectHttpResponse = apiClient.createEffect(createEffectRequest)
        createEffectHttpResponse.status shouldBe HttpStatus.CREATED
        val effectUuid = createEffectHttpResponse.body() as String

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        val createdEffect = effectEntities.first()
        createdEffect.uuid shouldBe effectUuid
        createdEffect.name shouldBe createEffectRequest.name
        createdEffect.pool?.uuid shouldBe pool.uuid
        createdEffect.strip shouldBe null
        createdEffect.status shouldBe LightEffectStatus.Inactive
        createdEffect.palette shouldBe palette
    }

    "Get effect by UUID" {
        val client = createLedStripClientEntity(clientRepository, "Test Room", "192.168.50.52", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Test Strip", 100, PiClientPin.D10.pinName, 100)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val createEffectRequest = CreateEffectRequest(
            stripUuid = strip.uuid,
            poolUuid = null,
            effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
            name = "Get Test Effect",
            settings = defaultNrSettings,
            paletteUuid = null,
            settingsUuid = null,
        )
        val createEffectHttpResponse = apiClient.createEffect(createEffectRequest)
        createEffectHttpResponse.status shouldBe HttpStatus.CREATED
        val effectUuid = createEffectHttpResponse.body() as String

        val getEffectResponse = apiClient.getEffect(effectUuid)
        getEffectResponse.status shouldBe HttpStatus.OK
        val effect = getEffectResponse.body() as GetStripEffectResponse
        effect.uuid shouldBe effectUuid
        effect.name shouldBe "Get Test Effect"
        effect.stripUuid shouldBe strip.uuid
    }

    "Create effect with invalid parameters returns 400" {
        val invalidRequest = CreateEffectRequest(
            stripUuid = null,
            poolUuid = null,
            effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
            name = "Invalid Effect",
            settings = emptyMap(),
            paletteUuid = null,
            settingsUuid = null,
        )
        val response = apiClient.createEffect(invalidRequest)
        response.status shouldBe HttpStatus.BAD_REQUEST
    }

    "Get non-existent effect returns 404" {
        val nonExistentUuid = UUID.randomUUID().toString()
        val response = apiClient.getEffect(nonExistentUuid)
        response.status shouldBe HttpStatus.NOT_FOUND
    }

    "Update non-existent effect returns 404" {
        val nonExistentUuid = UUID.randomUUID().toString()
        val updateRequest = UpdateEffectRequest(
            name = "Non-existent Update",
            stripUuid = null,
            poolUuid = null,
            paletteUuid = null,
            settingsUuid = null,
        )
        val response = apiClient.updateEffect(nonExistentUuid, updateRequest)
        response.status shouldBe HttpStatus.NOT_FOUND
    }

    "Delete non-existent effect returns 404" {
        val nonExistentUuid = UUID.randomUUID().toString()
        val response = apiClient.deleteEffect(nonExistentUuid)
        response.status shouldBe HttpStatus.NOT_FOUND
    }

    "Get effects for a LED strip pool" {
        val client = createLedStripClientEntity(clientRepository, "Backyard lights", "192.168.50.55", 50, 51)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D10.pinName, 100)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 150, PiClientPin.D21.pinName, 80)

        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Backyard Pool",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripA, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripB, pool = pool, uuid = UUID.randomUUID().toString(),poolIndex = 1, inverted = false)
        )

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
                name = "Blue palette",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val nrSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        val effect1 = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Pool Effect 1",
                effectSettings = nrSettingsEntity,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
            )
        )
        val effect2 = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Pool Effect 2",
                effectSettings = nrSettingsEntity,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
                layer = 1,
            )
        )

        val getEffectsResponse = apiClient.getEffects(null, pool.uuid)
        getEffectsResponse.status shouldBe HttpStatus.OK

        val effectsResponse = getEffectsResponse.body() as GetEffectsResponse
        effectsResponse.effects.size shouldBe 2

        val poolEffects = effectsResponse.effects.map { it as GetPoolEffectResponse }
        poolEffects.map { it.poolUuid } shouldBe listOf(pool.uuid, pool.uuid)
        poolEffects.map { it.uuid }.toSet() shouldBe setOf(effect1.uuid, effect2.uuid)
        poolEffects.map { it.name }.toSet() shouldBe setOf("Pool Effect 1", "Pool Effect 2")
    }

    "Get a pool effect by UUID" {
        val client = createLedStripClientEntity(clientRepository, "Garage lights", "192.168.50.56", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)

        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Garage Pool",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = strip, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )

        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(
                    primaryColor = RgbColor.Yellow,
                    secondaryColor = RgbColor.Yellow,
                    tertiaryColor = null,
                    otherColors = listOf()
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Yellow palette",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val nrSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        val poolEffect = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Garage Pool Effect",
                effectSettings = nrSettingsEntity,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
            )
        )

        val getEffectResponse = apiClient.getEffect(poolEffect.uuid)
        getEffectResponse.status shouldBe HttpStatus.OK

        val effect = getEffectResponse.body() as GetPoolEffectResponse
        effect.uuid shouldBe poolEffect.uuid
        effect.name shouldBe "Garage Pool Effect"
        effect.poolUuid shouldBe pool.uuid
        effect.paletteUuid shouldBe palette.uuid
    }

    "Update a pool effect" {
        val client = createLedStripClientEntity(clientRepository, "Deck lights", "192.168.50.57", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)

        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Deck Pool",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Additive
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = strip, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )

        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(
                    primaryColor = RgbColor.Purple,
                    secondaryColor = RgbColor.Purple,
                    tertiaryColor = null,
                    otherColors = listOf()
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Purple palette",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val updatedNrSettings = objectToMap(
            objectMapper,
            NightriderColorFillEffectSettings().copy(wrap = true)
        )
        val initialSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Initial NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        val updatedSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Updated NR Settings",
                settings = updatedNrSettings,
                isDefault = false,
            )
        )
        var poolEffect = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Original Pool Effect",
                effectSettings = initialSettingsEntity,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
            )
        )

        val updateRequest = UpdateEffectRequest(
            name = "Updated Pool Effect Name",
            stripUuid = null,
            poolUuid = pool.uuid,
            paletteUuid = null,
            settingsUuid = updatedSettingsEntity.uuid,
        )
        val updateResponse = apiClient.updateEffect(poolEffect.uuid, updateRequest)
        updateResponse.status shouldBe HttpStatus.NO_CONTENT

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        val updatedEffect = effectEntities.first()
        updatedEffect.name shouldBe "Updated Pool Effect Name"
        updatedEffect.uuid shouldBe poolEffect.uuid
        updatedEffect.pool?.uuid shouldBe pool.uuid
        updatedEffect.strip shouldBe null
        updatedEffect.effectSettings!!.settings.map { normalizeNumberTypes(it.value) } shouldBe updatedNrSettings.map {
            normalizeNumberTypes(
                it.value
            )
        }
    }

    "Delete a pool effect" {
        val client = createLedStripClientEntity(clientRepository, "Patio lights", "192.168.50.58", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 100, PiClientPin.D10.pinName, 100)

        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Patio Pool",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = strip, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )

        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(
                    primaryColor = RgbColor.Orange,
                    secondaryColor = RgbColor.Orange,
                    tertiaryColor = null,
                    otherColors = listOf()
                )
            )
        )
        val palette = paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = "Orange palette",
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )

        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val nrSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Test NR Settings",
                settings = defaultNrSettings,
                isDefault = false,
            )
        )
        val poolEffect = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Patio Pool Effect",
                effectSettings = nrSettingsEntity,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Inactive,
            )
        )

        val deleteResponse = apiClient.deleteEffect(poolEffect.uuid)
        deleteResponse.status shouldBe HttpStatus.NO_CONTENT

        effectRepository.findAll().isEmpty() shouldBe true
        paletteRepository.findAll().isEmpty() shouldBe false
        poolRepository.findAll().isEmpty() shouldBe false
    }

    "Consolidated getEffects - returns empty list when no effects match filter" {
        val client = createLedStripClientEntity(clientRepository, "Unused", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip", 100, PiClientPin.D21.pinName, 80)

        val response = apiClient.getEffects(strip.uuid, null)
        response.status shouldBe HttpStatus.OK
        val effectsResponse = response.body() as GetEffectsResponse
        effectsResponse.effects.isEmpty() shouldBe true
    }

    "getEffects endpoint - handles empty results gracefully" {
        val response = apiClient.getEffects(null, null)
        response.status shouldBe HttpStatus.OK
        val body = response.body() as GetEffectsResponse
        body.effects.isEmpty() shouldBe true
    }

    "Get effect schemas returns one schema per effect" {
        val response = apiClient.getSchemas()
        response.status shouldBe HttpStatus.OK
        val body = response.body() as List<*>
        // EffectApiServiceTest verifies details
        body.size shouldBe 8
    }

    "Create an effect with an explicit layer persists it and is reflected in the response" {
        val client = createLedStripClientEntity(clientRepository, "Office lights", "192.168.50.60", 60, 61)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val createResponse = apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Layer 3 effect",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = 3,
            )
        )
        createResponse.status shouldBe HttpStatus.CREATED
        val createdUuid = createResponse.body() as String

        effectRepository.findByUuid(createdUuid).get().layer shouldBe 3

        val getResponse = apiClient.getEffect(createdUuid)
        getResponse.status shouldBe HttpStatus.OK
        (getResponse.body() as GetStripEffectResponse).layer shouldBe 3
    }

    "Create an effect on the same strip with a duplicate layer is rejected" {
        val client = createLedStripClientEntity(clientRepository, "Garage lights", "192.168.50.70", 70, 71)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "First",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = 0,
            )
        ).status shouldBe HttpStatus.CREATED

        val secondResponse = apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Second",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = 0,
            )
        )

        secondResponse.status shouldBe HttpStatus.BAD_REQUEST
        effectRepository.queryAll().size shouldBe 1
    }

    "Create an effect without a layer auto-assigns the next free layer on the strip" {
        val client = createLedStripClientEntity(clientRepository, "Kitchen lights", "192.168.50.80", 80, 81)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        fun create() = apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Auto layer",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = null,
            )
        )

        val firstUuid = create().body() as String
        val secondUuid = create().body() as String
        val thirdUuid = create().body() as String

        effectRepository.findByUuid(firstUuid).get().layer shouldBe 0
        effectRepository.findByUuid(secondUuid).get().layer shouldBe 1
        effectRepository.findByUuid(thirdUuid).get().layer shouldBe 2
    }

    "Create auto-layer slots above an explicitly assigned high layer" {
        val client = createLedStripClientEntity(clientRepository, "Studio lights", "192.168.50.90", 90, 91)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Explicit layer 5",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = 5,
            )
        ).status shouldBe HttpStatus.CREATED

        val autoUuid = apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Auto after 5",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = null,
            )
        ).body() as String

        effectRepository.findByUuid(autoUuid).get().layer shouldBe 6
    }

    "Patching an effect without a layer recomputes to max-of-other-effects + 1 on its strip" {
        val client = createLedStripClientEntity(clientRepository, "Lab lights", "192.168.50.100", 100, 101)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val anchorUuid = apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Anchor on layer 4",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = 4,
            )
        ).body() as String

        val targetUuid = apiClient.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Target on layer 1",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
                layer = 1,
            )
        ).body() as String

        apiClient.updateEffect(
            targetUuid,
            UpdateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                paletteUuid = null,
                settingsUuid = null,
                name = null,
                layer = null,
            ),
        ).status shouldBe HttpStatus.NO_CONTENT

        effectRepository.findByUuid(anchorUuid).get().layer shouldBe 4
        effectRepository.findByUuid(targetUuid).get().layer shouldBe 5
    }

    "Effect settings preserve skipFramesIfBlank through create and patch" {
        val createBody = io.cyborgsquirrel.lighting.effects.requests.CreateEffectSettingsRequest(
            type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
            name = "Skip blank off",
            settings = objectToMap(objectMapper, NightriderColorFillEffectSettings()),
            isDefault = false,
            skipFramesIfBlank = false,
        )
        val createResponse = apiClient.createEffectSettings(createBody)
        createResponse.status shouldBe HttpStatus.CREATED
        val settingsUuid = createResponse.body() as String

        val initial = apiClient.getEffectSettings(settingsUuid).body()
                as io.cyborgsquirrel.lighting.effects.responses.GetEffectSettingsResponse
        initial.skipFramesIfBlank shouldBe false

        val patchBody = io.cyborgsquirrel.lighting.effects.requests.UpdateEffectSettingsRequest(
            name = null,
            settings = null,
            isDefault = null,
            skipFramesIfBlank = true,
        )
        apiClient.updateEffectSettings(settingsUuid, patchBody).status shouldBe HttpStatus.NO_CONTENT

        val patched = apiClient.getEffectSettings(settingsUuid).body()
                as io.cyborgsquirrel.lighting.effects.responses.GetEffectSettingsResponse
        patched.skipFramesIfBlank shouldBe true
    }
})