package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.repository.H2PoolMemberLedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.EffectPaletteConstants
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_palette.settings.*
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.api.EffectApi
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetPoolEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
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
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.annotation.Client
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.util.*

@MicronautTest
class EffectControllerTest(
    @Client private val apiClient: EffectApi,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val poolRepository: H2LedStripPoolRepository,
    private val poolMemberRepository: H2PoolMemberLedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val paletteRepository: H2LightEffectPaletteRepository,
    private val objectMapper: ObjectMapper
) : StringSpec({

    afterEach {
        paletteRepository.deleteAll()
        effectRepository.deleteAll()
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
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var effectEntity = LightEffectEntity(
            strip = strip,
            palette = palette,
            name = "Super cool effect",
            type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Idle,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        val getAllEffectsHttpResponse = apiClient.getAllEffects()
        getAllEffectsHttpResponse.status shouldBe HttpStatus.OK

        val getAllEffectsResponse = getAllEffectsHttpResponse.body()
        getAllEffectsResponse.effects.size shouldBe 1
        val effectFromApi = getAllEffectsResponse.effects.first()

        effectFromApi.name shouldBe effectEntity.name
        effectFromApi.uuid shouldBe effectEntity.uuid
        effectFromApi.status shouldBe effectEntity.status
        effectFromApi.settings.map { normalizeNumberTypes(it.value) } shouldBe effectEntity.settings!!.map {
            normalizeNumberTypes(
                it.value
            )
        }
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
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var effectEntity = LightEffectEntity(
            strip = strips.last(),
            palette = palette,
            name = "Super cool effect",
            type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Idle,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        var getAllEffectsHttpResponse = apiClient.getEffectsForStrip(strips.first().uuid, null)
        getAllEffectsHttpResponse.status shouldBe HttpStatus.OK

        var getAllEffectsResponse = getAllEffectsHttpResponse.body() as GetEffectsResponse
        getAllEffectsResponse.effects.isEmpty() shouldBe true

        getAllEffectsHttpResponse = apiClient.getEffectsForStrip(strips.last().uuid, null)
        getAllEffectsResponse = getAllEffectsHttpResponse.body() as GetEffectsResponse
        getAllEffectsResponse.effects.size shouldBe 1
        val effectFromApi = getAllEffectsResponse.effects.first()

        effectFromApi.name shouldBe effectEntity.name
        effectFromApi.uuid shouldBe effectEntity.uuid
        effectFromApi.status shouldBe effectEntity.status
        effectFromApi.settings.map { normalizeNumberTypes(it.value) } shouldBe effectEntity.settings!!.map {
            normalizeNumberTypes(
                it.value
            )
        }
        effectFromApi::class.java shouldBe GetStripEffectResponse::class.java
        (effectFromApi as GetStripEffectResponse).stripUuid shouldBe effectEntity.strip?.uuid
        effectFromApi.paletteUuid shouldBe palette?.uuid
    }

    "Create an effect" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val request = CreateEffectRequest(
            strip.uuid!!,
            null,
            LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            "Rainbow Nightrider",
            defaultNrSettings,
            paletteUuid = null,
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
        effectEntity.settings!!.map { normalizeNumberTypes(it.value) } shouldBe request.settings.map {
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
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val updatedNrSettings = objectToMap(
            objectMapper,
            NightriderEffectSettings.default().copy(wrap = true)
        )
        var effectEntity = LightEffectEntity(
            strip = strip,
            palette = palette,
            name = "Super cool light effect",
            type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Idle,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        val updateRequest = UpdateEffectRequest(
            settings = updatedNrSettings,
            stripUuid = null,
            poolUuid = null,
            paletteUuid = null,
            name = "New effect name",
        )
        val updateRequestHttpResponse = apiClient.updateEffect(effectEntity.uuid!!, updateRequest)
        updateRequestHttpResponse.status shouldBe HttpStatus.NO_CONTENT

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        effectEntities.first().strip?.uuid shouldBe strip.uuid
        effectEntities.first().name shouldBe updateRequest.name
        effectEntities.first().uuid shouldBe effectEntity.uuid
        effectEntities.first().settings!!.map { normalizeNumberTypes(it.value) } shouldBe updateRequest.settings!!.map {
            normalizeNumberTypes(
                it.value
            )
        }
        effectEntities.first().palette shouldBe palette
        // Update effect API doesn't support this - need to use updateEffectStatuses (/status) instead
        effectEntities.first().status shouldBe LightEffectStatus.Idle
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
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var effectEntity = LightEffectEntity(
            strip = strip,
            palette = palette,
            name = "Super cool effect",
            type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            uuid = UUID.randomUUID().toString(),
            status = LightEffectStatus.Idle,
            settings = defaultNrSettings
        )
        effectEntity = effectRepository.save(effectEntity)

        val createEffectHttpResponse = apiClient.deleteEffect(effectEntity.uuid!!)
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
            PoolMemberLedStripEntity(strip = stripA, pool = pool, poolIndex = 0, inverted = false)
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripB, pool = pool, poolIndex = 1, inverted = false)
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

        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val createEffectRequest = CreateEffectRequest(
            stripUuid = null,
            poolUuid = pool.uuid,
            effectType = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            name = "Pool Nightrider Effect",
            settings = defaultNrSettings,
            paletteUuid = palette.uuid
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
        createdEffect.status shouldBe LightEffectStatus.Idle
        createdEffect.palette shouldBe palette
    }

    "Get effect by UUID" {
        val client = createLedStripClientEntity(clientRepository, "Test Room", "192.168.50.52", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Test Strip", 100, PiClientPin.D10.pinName, 100)
        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val createEffectRequest = CreateEffectRequest(
            stripUuid = strip.uuid,
            poolUuid = null,
            effectType = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            name = "Get Test Effect",
            settings = defaultNrSettings,
            paletteUuid = null,
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
            effectType = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
            name = "Invalid Effect",
            settings = emptyMap(),
            paletteUuid = null,
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
            settings = emptyMap(),
            stripUuid = null,
            poolUuid = null,
            paletteUuid = null
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
            PoolMemberLedStripEntity(strip = stripA, pool = pool, poolIndex = 0, inverted = false)
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripB, pool = pool, poolIndex = 1, inverted = false)
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

        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val effect1 = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Pool Effect 1",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = defaultNrSettings
            )
        )
        val effect2 = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Pool Effect 2",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = defaultNrSettings
            )
        )

        val getEffectsResponse = apiClient.getEffectsForStrip(null, pool.uuid)
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
            PoolMemberLedStripEntity(strip = strip, pool = pool, poolIndex = 0, inverted = false)
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

        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val poolEffect = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Garage Pool Effect",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = defaultNrSettings
            )
        )

        val getEffectResponse = apiClient.getEffect(poolEffect.uuid!!)
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
            PoolMemberLedStripEntity(strip = strip, pool = pool, poolIndex = 0, inverted = false)
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

        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        var poolEffect = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Original Pool Effect",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = defaultNrSettings
            )
        )

        val updatedNrSettings = objectToMap(
            objectMapper,
            NightriderEffectSettings.default().copy(wrap = true)
        )
        val updateRequest = UpdateEffectRequest(
            name = "Updated Pool Effect Name",
            settings = updatedNrSettings,
            stripUuid = null,
            poolUuid = null,
            paletteUuid = null
        )
        val updateResponse = apiClient.updateEffect(poolEffect.uuid!!, updateRequest)
        updateResponse.status shouldBe HttpStatus.NO_CONTENT

        val effectEntities = effectRepository.queryAll()
        effectEntities.size shouldBe 1

        val updatedEffect = effectEntities.first()
        updatedEffect.name shouldBe "Updated Pool Effect Name"
        updatedEffect.uuid shouldBe poolEffect.uuid
        updatedEffect.pool?.uuid shouldBe pool.uuid
        updatedEffect.strip shouldBe null
        updatedEffect.settings!!.map { normalizeNumberTypes(it.value) } shouldBe updatedNrSettings.map {
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
            PoolMemberLedStripEntity(strip = strip, pool = pool, poolIndex = 0, inverted = false)
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

        val defaultNrSettings = objectToMap(objectMapper, NightriderEffectSettings.default())
        val poolEffect = effectRepository.save(
            LightEffectEntity(
                pool = pool,
                palette = palette,
                name = "Patio Pool Effect",
                type = LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                uuid = UUID.randomUUID().toString(),
                status = LightEffectStatus.Idle,
                settings = defaultNrSettings
            )
        )

        val deleteResponse = apiClient.deleteEffect(poolEffect.uuid!!)
        deleteResponse.status shouldBe HttpStatus.NO_CONTENT

        effectRepository.findAll().isEmpty() shouldBe true
        paletteRepository.findAll().isEmpty() shouldBe false
        poolRepository.findAll().isEmpty() shouldBe false
    }
})