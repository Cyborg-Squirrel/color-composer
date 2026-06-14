package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
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
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.requests.ReassignEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.responses.GetPoolEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsSchemaField
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsType
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsValidator
import io.cyborgsquirrel.lighting.effects.settings.BouncingBallEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.FlameEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.MarqueeEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.SparkleEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.WaveEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.normalizeNumberTypes
import io.cyborgsquirrel.test_helpers.objectToMap
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*
import kotlin.math.max

@MicronautTest
class EffectApiServiceTest(
    private val effectApiService: EffectApiService,
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val poolRepository: LedStripPoolRepository,
    private val poolMemberRepository: PoolMemberLedStripRepository,
    private val effectRepository: LightEffectRepository,
    private val settingsRepository: LightEffectSettingsRepository,
    private val paletteRepository: LightEffectPaletteRepository,
    private val objectMapper: ObjectMapper,
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

    fun makeCreate(
        strip: LedStripEntity,
        name: String,
        settings: Map<String, Any>,
        layer: Int? = null,
        paletteUuid: String? = null,
        settingsUuid: String? = null,
    ) = CreateEffectRequest(
        stripUuid = strip.uuid,
        poolUuid = null,
        effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
        name = name,
        settings = settings,
        paletteUuid = paletteUuid,
        settingsUuid = settingsUuid,
        layer = layer,
    )

    fun makeCreateForPool(
        pool: LedStripPoolEntity,
        name: String,
        settings: Map<String, Any>,
        layer: Int? = null,
        paletteUuid: String? = null,
        settingsUuid: String? = null,
    ) = CreateEffectRequest(
        stripUuid = null,
        poolUuid = pool.uuid,
        effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
        name = name,
        settings = settings,
        paletteUuid = paletteUuid,
        settingsUuid = settingsUuid,
        layer = layer,
    )

    fun savePalette(name: String): LightEffectPaletteEntity {
        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(primaryColor = RgbColorPresets.red(), secondaryColor = RgbColorPresets.red(), tertiaryColor = null, otherColors = listOf())
            )
        )
        return paletteRepository.save(
            LightEffectPaletteEntity(
                uuid = UUID.randomUUID().toString(),
                settings = paletteSettings,
                name = name,
                type = EffectPaletteConstants.STATIC_COLOR_PALETTE,
            )
        )
    }

    fun createPool(
        name: String,
        clientAddress: String,
        apiPort: Int,
        wsPort: Int,
    ): Pair<LedStripPoolEntity, LedStripEntity> {
        val client = createLedStripClientEntity(clientRepository, "$name client", clientAddress, apiPort, wsPort)
        val strip = saveLedStrip(stripRepository, client, "$name strip", 60, PiClientPin.D21.pinName, 80)
        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = name,
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average,
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = strip, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )
        return pool to strip
    }

    // ---------------------------------------------------------------------
    // Schemas
    // ---------------------------------------------------------------------

    "getAllSchemas returns one schema per effect" {
        effectApiService.getAllSchemas().size shouldBe 8
    }

    "all schema fields have non-blank keys and descriptions" {
        effectApiService.getAllSchemas().flatMap { it.fields }.forEach { field ->
            field.key.isNotBlank() shouldBe true
            field.description.isNotBlank() shouldBe true
        }
    }

    "each effect schema has the correct fields" {
        data class Field(val key: String, val type: EffectSettingsType, val default: Any?)
        data class Case(val effectName: String, val expectedFields: List<Field>)

        val schemas = effectApiService.getAllSchemas()

        listOf(
            Case(
                LightEffectType.SPECTRUM.displayName, listOf(
                    Field("colorBandPercentage", EffectSettingsType.Integer, 10),
                    Field("animated", EffectSettingsType.Boolean, true),
                    Field("updatesPerSecond", EffectSettingsType.Integer, 30),
                )
            ),
            Case(
                LightEffectType.NIGHTRIDER_COLOR_FILL.displayName, listOf(
                    Field("wrap", EffectSettingsType.Boolean, false),
                    Field("updatesPerSecond", EffectSettingsType.Integer, 35),
                    Field("brightnessScaling", EffectSettingsType.Number, 0.2f),
                )
            ),
            Case(
                LightEffectType.NIGHTRIDER_COMET.displayName, listOf(
                    Field("trailLength", EffectSettingsType.Integer, 5),
                    Field("trailFadeCurve", EffectSettingsType.String, FadeCurve.Linear.name),
                    Field("wrap", EffectSettingsType.Boolean, false),
                    Field("updatesPerSecond", EffectSettingsType.Integer, 35),
                )
            ),
            Case(
                LightEffectType.FLAME.displayName, listOf(
                    Field("cooling", EffectSettingsType.Integer, 11),
                    Field("sparking", EffectSettingsType.Integer, 140),
                    Field("sparks", EffectSettingsType.Integer, 1),
                    Field("sparkHeight", EffectSettingsType.Integer, 3),
                    Field("updatesPerSecond", EffectSettingsType.Integer, 30),
                )
            ),
            Case(
                LightEffectType.BOUNCING_BALL.displayName, listOf(
                    Field("startingHeightPercent", EffectSettingsType.Integer, 1),
                    Field("maxHeightPercent", EffectSettingsType.Integer, 100),
                    Field("speed", EffectSettingsType.Number, 4.0),
                    Field("gravity", EffectSettingsType.Number, LightEffectConstants.EARTH_GRAVITY),
                    Field("minimumSpeed", EffectSettingsType.Number, 0.05),
                )
            ),
            Case(
                LightEffectType.WAVE.displayName, listOf(
                    Field("startPointPercentage", EffectSettingsType.Integer, 50),
                    Field("waveLength", EffectSettingsType.Integer, 10),
                    Field("repeat", EffectSettingsType.Boolean, false),
                    Field("updatesPerSecond", EffectSettingsType.Integer, 30),
                )
            ),
            Case(
                LightEffectType.MARQUEE.displayName, listOf(
                    Field("dotLength", EffectSettingsType.Integer, 2),
                    Field("spaceBetweenDots", EffectSettingsType.Integer, 2),
                    Field("updatesPerSecond", EffectSettingsType.Integer, 8),
                )
            ),
            Case(
                LightEffectType.SPARKLE.displayName, listOf(
                    Field("numDots", EffectSettingsType.Integer, 10),
                    Field("fadeInMillisMax", EffectSettingsType.Integer, 10),
                    Field("fadeInMillisMin", EffectSettingsType.Integer, 5),
                    Field("fadeOutMillisMax", EffectSettingsType.Integer, 1000),
                    Field("fadeOutMillisMin", EffectSettingsType.Integer, 150),
                    Field("updatesPerSecond", EffectSettingsType.Integer, 30),
                )
            ),
        ).forEach { (effectName, expectedFields) ->
            val schema = schemas.first { it.effectName == effectName }
            schema.fields.map { Field(it.key, it.type, it.default) } shouldBe expectedFields
        }
    }

    "fields with range validators have correct min and max" {
        data class Case(val effectName: String, val fieldKey: String, val min: Double?, val max: Double?)

        val schemas = effectApiService.getAllSchemas()

        listOf(
            Case(LightEffectType.NIGHTRIDER_COLOR_FILL.displayName, "brightnessScaling", 0.0, 1.0),
            Case(LightEffectType.FLAME.displayName, "sparking", 0.0, 255.0),
            Case(LightEffectType.BOUNCING_BALL.displayName, "startingHeightPercent", 0.0, 100.0),
        ).forEach { (effectName, fieldKey, expectedMin, expectedMax) ->
            val field = schemas.first { it.effectName == effectName }.fields.first { it.key == fieldKey }
            field.validators.filterIsInstance<EffectSettingsValidator.Min>().firstOrNull()?.value shouldBe expectedMin
            field.validators.filterIsInstance<EffectSettingsValidator.Max>().firstOrNull()?.value shouldBe expectedMax
        }
    }

    "trailFadeCurve field has options for all FadeCurve values" {
        val cometSchema =
            effectApiService.getAllSchemas().first { it.effectName == LightEffectType.NIGHTRIDER_COMET.displayName }
        val field = cometSchema.fields.first { it.key == "trailFadeCurve" }
        val options = field.validators.filterIsInstance<EffectSettingsValidator.Options>().first()
        options.values shouldBe listOf("Linear", "Logarithmic")
    }

    "schema fields deserialize back into their effect settings classes" {
        fun jsonValueFor(field: EffectSettingsSchemaField<*>): String {
            val min = field.validators.filterIsInstance<EffectSettingsValidator.Min>().firstOrNull()?.value
            val max = field.validators.filterIsInstance<EffectSettingsValidator.Max>().firstOrNull()?.value
            val options = field.validators.filterIsInstance<EffectSettingsValidator.Options>().firstOrNull()
            return when (field.type) {
                EffectSettingsType.Integer ->
                    if (min != null && max != null) ((min + max) / 2).toInt().toString()
                    else max(min ?: 1.0, 1.0).toInt().toString()

                EffectSettingsType.Number ->
                    if (min != null && max != null) ((min + max) / 2).toString()
                    else max(min ?: 1.0, 0.1).toString()

                EffectSettingsType.Boolean -> "true"

                EffectSettingsType.String -> "\"${options?.values?.first() ?: "value"}\""
            }
        }

        data class Case(val effectType: LightEffectType, val settingsClass: Class<*>)
        val cases = listOf(
            Case(LightEffectType.SPECTRUM, SpectrumEffectSettings::class.java),
            Case(LightEffectType.NIGHTRIDER_COLOR_FILL, NightriderColorFillEffectSettings::class.java),
            Case(LightEffectType.NIGHTRIDER_COMET, NightriderCometEffectSettings::class.java),
            Case(LightEffectType.FLAME, FlameEffectSettings::class.java),
            Case(LightEffectType.BOUNCING_BALL, BouncingBallEffectSettings::class.java),
            Case(LightEffectType.WAVE, WaveEffectSettings::class.java),
            Case(LightEffectType.MARQUEE, MarqueeEffectSettings::class.java),
            Case(LightEffectType.SPARKLE, SparkleEffectSettings::class.java),
        )

        val schemas = effectApiService.getAllSchemas()

        cases.forEach { (effectType, settingsClass) ->
            val schema = schemas.first { it.effectName == effectType.displayName }
            val json = schema.fields.joinToString(",", "{", "}") { "\"${it.key}\":${jsonValueFor(it)}" }
            @Suppress("UNCHECKED_CAST")
            val result = objectMapper.readValue(json, settingsClass as Class<Any>)!!
            result::class.java shouldBe settingsClass
        }
    }

    // ---------------------------------------------------------------------
    // createEffect — success
    // ---------------------------------------------------------------------

    "createEffect persists an effect on a strip and resolves its settings" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val effectUuid = effectApiService.createEffect(
            CreateEffectRequest(
                stripUuid = strip.uuid,
                poolUuid = null,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Rainbow Nightrider",
                settings = defaultNrSettings,
                paletteUuid = null,
                settingsUuid = null,
            )
        )

        val entity = effectRepository.findByUuid(effectUuid).get()
        entity.strip?.uuid shouldBe strip.uuid
        entity.name shouldBe "Rainbow Nightrider"
        val entitySettings = entity.effectSettings.shouldNotBeNull()
        entitySettings.settings.map { normalizeNumberTypes(it.value) } shouldBe
                defaultNrSettings.map { normalizeNumberTypes(it.value) }
        entity.palette shouldBe null
    }

    "createEffect persists an effect on a pool with a palette" {
        val client = createLedStripClientEntity(clientRepository, "Living Room lights", "192.168.50.50", 50, 51)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D10.pinName, 100)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 150, PiClientPin.D21.pinName, 80)

        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Living Room Pool",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average,
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripA, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripB, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 1, inverted = false)
        )

        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(primaryColor = RgbColorPresets.green(), secondaryColor = RgbColorPresets.cyan(), tertiaryColor = null, otherColors = listOf())
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
        val effectUuid = effectApiService.createEffect(
            CreateEffectRequest(
                stripUuid = null,
                poolUuid = pool.uuid,
                effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Pool Nightrider Effect",
                settings = defaultNrSettings,
                paletteUuid = palette.uuid,
                settingsUuid = null,
            )
        )

        val created = effectRepository.findByUuid(effectUuid).get()
        created.name shouldBe "Pool Nightrider Effect"
        created.pool?.uuid shouldBe pool.uuid
        created.strip shouldBe null
        created.status shouldBe LightEffectStatus.Inactive
        created.palette shouldBe palette
    }

    "createEffect at explicit layer shifts later strip effects up" {
        val client = createLedStripClientEntity(clientRepository, "Garage lights", "192.168.50.70", 70, 71)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val firstUuid = effectApiService.createEffect(makeCreate(strip, "First", defaultNrSettings, layer = 0))
        val secondUuid = effectApiService.createEffect(makeCreate(strip, "Second", defaultNrSettings, layer = 0))

        effectRepository.queryAll().size shouldBe 2
        effectRepository.findByUuid(secondUuid).get().layer shouldBe 0
        effectRepository.findByUuid(firstUuid).get().layer shouldBe 1
    }

    "createEffect at explicit layer in a pool shifts later pool effects up" {
        val (pool, _) = createPool("Layered Pool", "192.168.50.141", 141, 142)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val first = effectApiService.createEffect(makeCreateForPool(pool, "First", defaultNrSettings))
        val second = effectApiService.createEffect(makeCreateForPool(pool, "Second", defaultNrSettings, layer = 0))

        effectRepository.findByUuid(second).get().layer shouldBe 0
        effectRepository.findByUuid(first).get().layer shouldBe 1
    }

    "createEffect without a layer auto-assigns the next free strip layer" {
        val client = createLedStripClientEntity(clientRepository, "Kitchen lights", "192.168.50.80", 80, 81)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val a = effectApiService.createEffect(makeCreate(strip, "A", defaultNrSettings))
        val b = effectApiService.createEffect(makeCreate(strip, "B", defaultNrSettings))
        val c = effectApiService.createEffect(makeCreate(strip, "C", defaultNrSettings))

        effectRepository.findByUuid(a).get().layer shouldBe 0
        effectRepository.findByUuid(b).get().layer shouldBe 1
        effectRepository.findByUuid(c).get().layer shouldBe 2
    }

    "createEffect without a layer auto-assigns the next free pool layer" {
        val (pool, _) = createPool("Auto Pool", "192.168.50.142", 142, 143)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val a = effectApiService.createEffect(makeCreateForPool(pool, "A", defaultNrSettings))
        val b = effectApiService.createEffect(makeCreateForPool(pool, "B", defaultNrSettings))
        val c = effectApiService.createEffect(makeCreateForPool(pool, "C", defaultNrSettings))

        effectRepository.findByUuid(a).get().layer shouldBe 0
        effectRepository.findByUuid(b).get().layer shouldBe 1
        effectRepository.findByUuid(c).get().layer shouldBe 2
    }

    "createEffect honors an explicit layer when it equals the current count" {
        val client = createLedStripClientEntity(clientRepository, "Office lights", "192.168.50.60", 60, 61)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        repeat(3) { i ->
            effectApiService.createEffect(makeCreate(strip, "Seed $i", defaultNrSettings))
        }

        val createdUuid = effectApiService.createEffect(makeCreate(strip, "Layer 3 effect", defaultNrSettings, layer = 3))

        effectRepository.findByUuid(createdUuid).get().layer shouldBe 3
    }

    // ---------------------------------------------------------------------
    // createEffect — validation
    // ---------------------------------------------------------------------

    "createEffect throws ClientRequestException when neither stripUuid nor poolUuid is provided" {
        shouldThrow<ClientRequestException> {
            effectApiService.createEffect(
                CreateEffectRequest(
                    stripUuid = null,
                    poolUuid = null,
                    effectType = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                    name = "Invalid",
                    settings = emptyMap(),
                    paletteUuid = null,
                    settingsUuid = null,
                )
            )
        }
    }

    "createEffect throws ClientRequestException when layer is beyond the current count" {
        val client = createLedStripClientEntity(clientRepository, "Cellar lights", "192.168.50.116", 110, 111)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        shouldThrow<ClientRequestException> {
            effectApiService.createEffect(makeCreate(strip, "Too high", defaultNrSettings, layer = 5))
        }
        effectRepository.queryAll().size shouldBe 0
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    "getAllEffects returns every effect across strips and pools" {
        val client = createLedStripClientEntity(clientRepository, "Porch lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 100)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val effectUuid = effectApiService.createEffect(makeCreate(strip, "Super cool effect", defaultNrSettings))

        val effects = effectApiService.getAllEffects().effects
        effects.size shouldBe 1
        val effectFromApi = effects.first()
        effectFromApi.uuid shouldBe effectUuid
        effectFromApi.name shouldBe "Super cool effect"
        effectFromApi::class.java shouldBe GetStripEffectResponse::class.java
        (effectFromApi as GetStripEffectResponse).stripUuid shouldBe strip.uuid
    }

    "getEffectsForStrip returns only effects on that strip" {
        val client = createLedStripClientEntity(clientRepository, "Living Room lights", "192.168.50.50", 50, 51)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D10.pinName, 100)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 100, PiClientPin.D21.pinName, 75)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val effectUuid = effectApiService.createEffect(makeCreate(stripB, "Cool effect", defaultNrSettings))

        effectApiService.getEffectsForStrip(stripA.uuid).effects.isEmpty() shouldBe true
        val stripBEffects = effectApiService.getEffectsForStrip(stripB.uuid).effects
        stripBEffects.size shouldBe 1
        stripBEffects.first().uuid shouldBe effectUuid
    }

    "getEffectsForPool returns only effects on that pool" {
        val (pool, _) = createPool("Backyard Pool", "192.168.50.55", 55, 56)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val first = effectApiService.createEffect(makeCreateForPool(pool, "Pool Effect 1", defaultNrSettings))
        val second = effectApiService.createEffect(makeCreateForPool(pool, "Pool Effect 2", defaultNrSettings))

        val results = effectApiService.getEffectsForPool(pool.uuid).effects
        results.size shouldBe 2
        results.map { it as GetPoolEffectResponse }.map { it.poolUuid }.toSet() shouldBe setOf(pool.uuid)
        results.map { it.uuid }.toSet() shouldBe setOf(first, second)
    }

    "getEffectWithUuid returns the matching effect" {
        val client = createLedStripClientEntity(clientRepository, "Test Room", "192.168.50.52", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Test Strip", 100, PiClientPin.D10.pinName, 100)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val effectUuid = effectApiService.createEffect(makeCreate(strip, "Get Test Effect", defaultNrSettings))
        val response = effectApiService.getEffectWithUuid(effectUuid) as GetStripEffectResponse
        response.uuid shouldBe effectUuid
        response.name shouldBe "Get Test Effect"
        response.stripUuid shouldBe strip.uuid
    }

    "getEffectWithUuid throws ResourceNotFoundException when the uuid is unknown" {
        shouldThrow<ResourceNotFoundException> {
            effectApiService.getEffectWithUuid(UUID.randomUUID().toString())
        }
    }

    // ---------------------------------------------------------------------
    // updateEffect — success
    // ---------------------------------------------------------------------

    "updateEffect renames the effect and swaps its settings" {
        val client = createLedStripClientEntity(clientRepository, "Bedroom lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 66)
        val paletteSettings = objectToMap(
            objectMapper,
            StaticPaletteSettings(
                SettingsPalette(primaryColor = RgbColorPresets.purple(), secondaryColor = RgbColorPresets.purple(), tertiaryColor = null, otherColors = listOf())
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
        val updatedNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings().copy(wrap = true))
        val updatedSettingsEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "Updated NR Settings",
                settings = updatedNrSettings,
                isDefault = false,
            )
        )
        val effectUuid = effectApiService.createEffect(
            makeCreate(strip, "Original", defaultNrSettings, paletteUuid = palette.uuid)
        )

        effectApiService.updateEffect(
            effectUuid,
            UpdateEffectRequest(
                paletteUuid = null,
                settingsUuid = updatedSettingsEntity.uuid,
                name = "New effect name",
            )
        )

        val updated = effectRepository.findByUuid(effectUuid).get()
        updated.name shouldBe "New effect name"
        updated.palette?.uuid shouldBe palette.uuid
        updated.effectSettings?.uuid shouldBe updatedSettingsEntity.uuid
        updated.status shouldBe LightEffectStatus.Inactive
    }

    "updateEffect with unassignPalette clears the palette" {
        val client = createLedStripClientEntity(clientRepository, "Palette unassign lights", "192.168.50.118", 118, 119)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val palette = savePalette("Red palette")

        val effectUuid = effectApiService.createEffect(
            makeCreate(strip, "E", defaultNrSettings, paletteUuid = palette.uuid)
        )

        effectApiService.updateEffect(
            effectUuid,
            UpdateEffectRequest(unassignPalette = true, paletteUuid = null, settingsUuid = null, name = null, layer = null),
        )

        effectRepository.findByUuid(effectUuid).get().palette shouldBe null
    }

    "updateEffect swapping only the palette preserves the layer" {
        val client = createLedStripClientEntity(clientRepository, "Palette swap lights", "192.168.50.145", 145, 146)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val initialPalette = savePalette("Blue")
        val newPalette = savePalette("Blue 2")

        effectApiService.createEffect(makeCreate(strip, "First", defaultNrSettings))
        val target = effectApiService.createEffect(
            makeCreate(strip, "Target", defaultNrSettings, paletteUuid = initialPalette.uuid)
        )
        effectApiService.createEffect(makeCreate(strip, "Third", defaultNrSettings))

        effectApiService.updateEffect(
            target,
            UpdateEffectRequest(paletteUuid = newPalette.uuid, settingsUuid = null, name = null, layer = null),
        )

        val updated = effectRepository.findByUuid(target).get()
        updated.palette?.uuid shouldBe newPalette.uuid
        updated.layer shouldBe 1
        updated.name shouldBe "Target"
    }

    "updateEffect leaves layer unchanged when layer field is null" {
        val client = createLedStripClientEntity(clientRepository, "Lab lights", "192.168.50.100", 100, 101)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val first = effectApiService.createEffect(makeCreate(strip, "First", defaultNrSettings))
        val target = effectApiService.createEffect(makeCreate(strip, "Target", defaultNrSettings))

        effectApiService.updateEffect(
            target,
            UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = "Renamed", layer = null),
        )

        effectRepository.findByUuid(first).get().layer shouldBe 0
        effectRepository.findByUuid(target).get().layer shouldBe 1
    }

    "updateEffect moving to layer 0 shifts the other strip effects up" {
        val client = createLedStripClientEntity(clientRepository, "Pantry lights", "192.168.50.110", 110, 111)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreate(strip, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreate(strip, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreate(strip, "Z", defaultNrSettings))

        effectApiService.updateEffect(
            z,
            UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = null, layer = 0),
        )

        effectRepository.findByUuid(z).get().layer shouldBe 0
        effectRepository.findByUuid(x).get().layer shouldBe 1
        effectRepository.findByUuid(y).get().layer shouldBe 2
    }

    "updateEffect moving a middle effect to layer 0 only shifts effects between the old and new positions" {
        val client = createLedStripClientEntity(clientRepository, "Closet lights", "192.168.50.111", 110, 111)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreate(strip, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreate(strip, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreate(strip, "Z", defaultNrSettings))

        effectApiService.updateEffect(
            y,
            UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = null, layer = 0),
        )

        effectRepository.findByUuid(y).get().layer shouldBe 0
        effectRepository.findByUuid(x).get().layer shouldBe 1
        effectRepository.findByUuid(z).get().layer shouldBe 2
    }

    "updateEffect moving an effect down to the end shifts effects above it" {
        val client = createLedStripClientEntity(clientRepository, "Hallway lights", "192.168.50.112", 110, 111)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreate(strip, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreate(strip, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreate(strip, "Z", defaultNrSettings))

        effectApiService.updateEffect(
            x,
            UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = null, layer = 2),
        )

        effectRepository.findByUuid(y).get().layer shouldBe 0
        effectRepository.findByUuid(z).get().layer shouldBe 1
        effectRepository.findByUuid(x).get().layer shouldBe 2
    }

    "updateEffect moving a pool effect to layer 0 shifts the other pool effects up" {
        val (pool, _) = createPool("Move Pool", "192.168.50.143", 143, 144)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreateForPool(pool, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreateForPool(pool, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreateForPool(pool, "Z", defaultNrSettings))

        effectApiService.updateEffect(
            z,
            UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = null, layer = 0),
        )

        effectRepository.findByUuid(z).get().layer shouldBe 0
        effectRepository.findByUuid(x).get().layer shouldBe 1
        effectRepository.findByUuid(y).get().layer shouldBe 2
    }

    "updateEffect with name, layer, palette, and settings together persists every change" {
        val client = createLedStripClientEntity(clientRepository, "Multi-field lights", "192.168.50.140", 140, 141)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val updatedNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings().copy(wrap = true))
        val palette = savePalette("Green palette")
        val newSettings = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
                name = "New NR Settings",
                settings = updatedNrSettings,
                isDefault = false,
            )
        )

        val first = effectApiService.createEffect(makeCreate(strip, "First", defaultNrSettings))
        val target = effectApiService.createEffect(makeCreate(strip, "Target", defaultNrSettings))
        val third = effectApiService.createEffect(makeCreate(strip, "Third", defaultNrSettings))

        effectApiService.updateEffect(
            target,
            UpdateEffectRequest(
                paletteUuid = palette.uuid,
                settingsUuid = newSettings.uuid,
                name = "Renamed Target",
                layer = 0,
            ),
        )

        val updated = effectRepository.findByUuid(target).get()
        updated.name shouldBe "Renamed Target"
        updated.layer shouldBe 0
        updated.palette?.uuid shouldBe palette.uuid
        updated.effectSettings?.uuid shouldBe newSettings.uuid

        effectRepository.findByUuid(first).get().layer shouldBe 1
        effectRepository.findByUuid(third).get().layer shouldBe 2
    }

    // ---------------------------------------------------------------------
    // updateEffect — validation
    // ---------------------------------------------------------------------

    "updateEffect throws ResourceNotFoundException when the effect uuid is unknown" {
        shouldThrow<ResourceNotFoundException> {
            effectApiService.updateEffect(
                UUID.randomUUID().toString(),
                UpdateEffectRequest(name = "Non-existent", paletteUuid = null, settingsUuid = null),
            )
        }
    }

    "updateEffect throws ClientRequestException when no fields are set" {
        val client = createLedStripClientEntity(clientRepository, "Empty req lights", "192.168.50.117", 117, 118)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.updateEffect(
                uuid,
                UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = null, layer = null),
            )
        }
    }

    "updateEffect throws ClientRequestException when paletteUuid is unknown" {
        val client = createLedStripClientEntity(clientRepository, "Bad palette lights", "192.168.50.137", 137, 138)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.updateEffect(
                uuid,
                UpdateEffectRequest(paletteUuid = UUID.randomUUID().toString(), settingsUuid = null, name = null, layer = null),
            )
        }
        effectRepository.findByUuid(uuid).get().palette shouldBe null
    }

    "updateEffect throws ClientRequestException when settingsUuid is unknown" {
        val client = createLedStripClientEntity(clientRepository, "Bad settings lights", "192.168.50.138", 138, 139)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))
        val originalSettingsUuid = effectRepository.findByUuid(uuid).get().effectSettings?.uuid

        shouldThrow<ClientRequestException> {
            effectApiService.updateEffect(
                uuid,
                UpdateEffectRequest(paletteUuid = null, settingsUuid = UUID.randomUUID().toString(), name = null, layer = null),
            )
        }
        effectRepository.findByUuid(uuid).get().effectSettings?.uuid shouldBe originalSettingsUuid
    }

    "updateEffect throws ClientRequestException when layer is beyond the current range" {
        val client = createLedStripClientEntity(clientRepository, "Attic lights", "192.168.50.115", 110, 111)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        effectApiService.createEffect(makeCreate(strip, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreate(strip, "Y", defaultNrSettings))
        effectApiService.createEffect(makeCreate(strip, "Z", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.updateEffect(
                y,
                UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = null, layer = 99),
            )
        }
    }

    "updateEffect throws ClientRequestException when layer is negative" {
        val client = createLedStripClientEntity(clientRepository, "Negative layer lights", "192.168.50.139", 139, 140)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.updateEffect(
                uuid,
                UpdateEffectRequest(paletteUuid = null, settingsUuid = null, name = null, layer = -1),
            )
        }
        effectRepository.findByUuid(uuid).get().layer shouldBe 0
    }

    "updateEffect throws ClientRequestException when unassignPalette is combined with a different paletteUuid" {
        val client = createLedStripClientEntity(clientRepository, "Palette conflict lights", "192.168.50.119", 119, 120)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val paletteA = savePalette("A")
        val paletteB = savePalette("B")

        val uuid = effectApiService.createEffect(
            makeCreate(strip, "E", defaultNrSettings, paletteUuid = paletteA.uuid)
        )

        shouldThrow<ClientRequestException> {
            effectApiService.updateEffect(
                uuid,
                UpdateEffectRequest(unassignPalette = true, paletteUuid = paletteB.uuid, settingsUuid = null, name = null, layer = null),
            )
        }
        effectRepository.findByUuid(uuid).get().palette?.uuid shouldBe paletteA.uuid
    }

    // ---------------------------------------------------------------------
    // reassignEffect — success
    // ---------------------------------------------------------------------

    "reassignEffect from strip to another strip compacts the source and appends to the destination" {
        val client = createLedStripClientEntity(clientRepository, "Loft lights", "192.168.50.114", 110, 111)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 60, PiClientPin.D10.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val ax = effectApiService.createEffect(makeCreate(stripA, "AX", defaultNrSettings))
        val ay = effectApiService.createEffect(makeCreate(stripA, "AY", defaultNrSettings))
        val az = effectApiService.createEffect(makeCreate(stripA, "AZ", defaultNrSettings))
        val bp = effectApiService.createEffect(makeCreate(stripB, "BP", defaultNrSettings))
        val bq = effectApiService.createEffect(makeCreate(stripB, "BQ", defaultNrSettings))

        effectApiService.reassignEffect(
            ay,
            ReassignEffectRequest(unassign = false, targetStripUuid = stripB.uuid, targetPoolUuid = null),
        )

        effectRepository.findByUuid(ax).get().layer shouldBe 0
        effectRepository.findByUuid(az).get().layer shouldBe 1
        effectRepository.findByUuid(bp).get().layer shouldBe 0
        effectRepository.findByUuid(bq).get().layer shouldBe 1
        effectRepository.findByUuid(ay).get().layer shouldBe 2
        effectRepository.findByUuid(ay).get().strip?.uuid shouldBe stripB.uuid
    }

    "reassignEffect from a strip to a pool compacts the strip and appends to the pool" {
        val client = createLedStripClientEntity(clientRepository, "Strip-to-pool lights", "192.168.50.130", 130, 131)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 60, PiClientPin.D10.pinName, 80)
        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Target Pool",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average,
            )
        )
        poolMemberRepository.save(
            PoolMemberLedStripEntity(strip = stripB, pool = pool, uuid = UUID.randomUUID().toString(), poolIndex = 0, inverted = false)
        )
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val sx = effectApiService.createEffect(makeCreate(stripA, "SX", defaultNrSettings))
        val sy = effectApiService.createEffect(makeCreate(stripA, "SY", defaultNrSettings))
        val sz = effectApiService.createEffect(makeCreate(stripA, "SZ", defaultNrSettings))
        val pp = effectApiService.createEffect(makeCreateForPool(pool, "PP", defaultNrSettings))

        effectApiService.reassignEffect(
            sy,
            ReassignEffectRequest(unassign = false, targetStripUuid = null, targetPoolUuid = pool.uuid),
        )

        val moved = effectRepository.findByUuid(sy).get()
        moved.pool?.uuid shouldBe pool.uuid
        moved.strip shouldBe null
        moved.layer shouldBe 1

        effectRepository.findByUuid(sx).get().layer shouldBe 0
        effectRepository.findByUuid(sz).get().layer shouldBe 1
        effectRepository.findByUuid(pp).get().layer shouldBe 0
    }

    "reassignEffect from a pool to a strip compacts the pool and appends to the strip" {
        val client = createLedStripClientEntity(clientRepository, "Pool-to-strip lights", "192.168.50.131", 131, 132)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 60, PiClientPin.D10.pinName, 80)
        val (pool, _) = createPool("Source Pool", "192.168.50.131", 131, 133)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val pa = effectApiService.createEffect(makeCreateForPool(pool, "PA", defaultNrSettings))
        val pb = effectApiService.createEffect(makeCreateForPool(pool, "PB", defaultNrSettings))
        val pc = effectApiService.createEffect(makeCreateForPool(pool, "PC", defaultNrSettings))
        val sa = effectApiService.createEffect(makeCreate(stripB, "SA", defaultNrSettings))

        effectApiService.reassignEffect(
            pa,
            ReassignEffectRequest(unassign = false, targetStripUuid = stripB.uuid, targetPoolUuid = null),
        )

        val moved = effectRepository.findByUuid(pa).get()
        moved.strip?.uuid shouldBe stripB.uuid
        moved.pool shouldBe null
        moved.layer shouldBe 1

        effectRepository.findByUuid(pb).get().layer shouldBe 0
        effectRepository.findByUuid(pc).get().layer shouldBe 1
        effectRepository.findByUuid(sa).get().layer shouldBe 0
    }

    "reassignEffect to the current strip is a no-op" {
        val client = createLedStripClientEntity(clientRepository, "No-op lights", "192.168.50.132", 132, 133)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreate(strip, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreate(strip, "Y", defaultNrSettings))

        effectApiService.reassignEffect(
            y,
            ReassignEffectRequest(unassign = false, targetStripUuid = strip.uuid, targetPoolUuid = null),
        )

        effectRepository.findByUuid(x).get().layer shouldBe 0
        effectRepository.findByUuid(y).get().layer shouldBe 1
        effectRepository.findByUuid(y).get().strip?.uuid shouldBe strip.uuid
    }

    "reassignEffect with unassign=true clears the strip and compacts the source" {
        val client = createLedStripClientEntity(clientRepository, "Unassign source lights", "192.168.50.121", 121, 122)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreate(strip, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreate(strip, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreate(strip, "Z", defaultNrSettings))

        effectApiService.reassignEffect(
            y,
            ReassignEffectRequest(unassign = true, targetStripUuid = null, targetPoolUuid = null),
        )

        effectRepository.findByUuid(y).get().strip shouldBe null
        effectRepository.findByUuid(y).get().pool shouldBe null
        effectRepository.findByUuid(x).get().layer shouldBe 0
        effectRepository.findByUuid(z).get().layer shouldBe 1
    }

    "reassignEffect with unassign=true on a pool effect clears the pool and compacts source" {
        val (pool, _) = createPool("Source Pool", "192.168.50.136", 136, 137)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreateForPool(pool, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreateForPool(pool, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreateForPool(pool, "Z", defaultNrSettings))

        effectApiService.reassignEffect(
            y,
            ReassignEffectRequest(unassign = true, targetStripUuid = null, targetPoolUuid = null),
        )

        effectRepository.findByUuid(y).get().pool shouldBe null
        effectRepository.findByUuid(y).get().strip shouldBe null
        effectRepository.findByUuid(x).get().layer shouldBe 0
        effectRepository.findByUuid(z).get().layer shouldBe 1
    }

    "reassignEffect of an unassigned effect to a strip places it at the next free layer" {
        val client = createLedStripClientEntity(clientRepository, "Unassigned-to-strip lights", "192.168.50.135", 135, 136)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 60, PiClientPin.D10.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val orphan = effectApiService.createEffect(makeCreate(stripA, "Orphan", defaultNrSettings))
        val bx = effectApiService.createEffect(makeCreate(stripB, "BX", defaultNrSettings))
        val by = effectApiService.createEffect(makeCreate(stripB, "BY", defaultNrSettings))

        effectApiService.reassignEffect(
            orphan,
            ReassignEffectRequest(unassign = true, targetStripUuid = null, targetPoolUuid = null),
        )
        effectRepository.findByUuid(orphan).get().strip shouldBe null
        effectRepository.findByUuid(orphan).get().pool shouldBe null

        effectApiService.reassignEffect(
            orphan,
            ReassignEffectRequest(unassign = false, targetStripUuid = stripB.uuid, targetPoolUuid = null),
        )

        effectRepository.findByUuid(orphan).get().strip?.uuid shouldBe stripB.uuid
        effectRepository.findByUuid(orphan).get().layer shouldBe 2
        effectRepository.findByUuid(bx).get().layer shouldBe 0
        effectRepository.findByUuid(by).get().layer shouldBe 1
    }

    // ---------------------------------------------------------------------
    // reassignEffect — validation
    // ---------------------------------------------------------------------

    "reassignEffect throws ResourceNotFoundException when the effect uuid is unknown" {
        shouldThrow<ResourceNotFoundException> {
            effectApiService.reassignEffect(
                UUID.randomUUID().toString(),
                ReassignEffectRequest(unassign = true, targetStripUuid = null, targetPoolUuid = null),
            )
        }
    }

    "reassignEffect throws ClientRequestException when the request specifies no target and no unassign" {
        val client = createLedStripClientEntity(clientRepository, "Reassign empty lights", "192.168.50.120", 120, 121)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.reassignEffect(
                uuid,
                ReassignEffectRequest(unassign = false, targetStripUuid = null, targetPoolUuid = null),
            )
        }
    }

    "reassignEffect throws ClientRequestException when both target strip and pool are provided" {
        val client = createLedStripClientEntity(clientRepository, "Both targets lights", "192.168.50.123", 123, 124)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val pool = poolRepository.save(
            LedStripPoolEntity(
                uuid = UUID.randomUUID().toString(),
                name = "Pool A",
                poolType = PoolType.Unified,
                blendMode = BlendMode.Average,
            )
        )
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.reassignEffect(
                uuid,
                ReassignEffectRequest(unassign = false, targetStripUuid = strip.uuid, targetPoolUuid = pool.uuid),
            )
        }
    }

    "reassignEffect throws ClientRequestException when the target strip is unknown" {
        val client = createLedStripClientEntity(clientRepository, "Bad target strip lights", "192.168.50.133", 133, 134)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.reassignEffect(
                uuid,
                ReassignEffectRequest(unassign = false, targetStripUuid = UUID.randomUUID().toString(), targetPoolUuid = null),
            )
        }
        effectRepository.findByUuid(uuid).get().strip?.uuid shouldBe strip.uuid
        effectRepository.findByUuid(uuid).get().layer shouldBe 0
    }

    "reassignEffect throws ClientRequestException when the target pool is unknown" {
        val client = createLedStripClientEntity(clientRepository, "Bad target pool lights", "192.168.50.134", 134, 135)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(strip, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.reassignEffect(
                uuid,
                ReassignEffectRequest(unassign = false, targetStripUuid = null, targetPoolUuid = UUID.randomUUID().toString()),
            )
        }
        effectRepository.findByUuid(uuid).get().strip?.uuid shouldBe strip.uuid
    }

    "reassignEffect throws ClientRequestException when unassign=true conflicts with a different target strip" {
        val client = createLedStripClientEntity(clientRepository, "Reassign conflict lights", "192.168.50.122", 122, 123)
        val stripA = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val stripB = saveLedStrip(stripRepository, client, "Strip B", 60, PiClientPin.D10.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val uuid = effectApiService.createEffect(makeCreate(stripA, "E", defaultNrSettings))

        shouldThrow<ClientRequestException> {
            effectApiService.reassignEffect(
                uuid,
                ReassignEffectRequest(unassign = true, targetStripUuid = stripB.uuid, targetPoolUuid = null),
            )
        }
        effectRepository.findByUuid(uuid).get().strip?.uuid shouldBe stripA.uuid
    }

    // ---------------------------------------------------------------------
    // deleteEffect
    // ---------------------------------------------------------------------

    "deleteEffect removes the effect and leaves palettes intact" {
        val client = createLedStripClientEntity(clientRepository, "Christmas Tree lights", "192.168.50.50", 50, 51)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 200, PiClientPin.D21.pinName, 50)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())
        val palette = savePalette("Tree palette")

        val effectUuid = effectApiService.createEffect(
            makeCreate(strip, "Super cool effect", defaultNrSettings, paletteUuid = palette.uuid)
        )

        effectApiService.deleteEffect(effectUuid)

        effectRepository.findAll().isEmpty() shouldBe true
        paletteRepository.findAll().isEmpty() shouldBe false
    }

    "deleteEffect throws ResourceNotFoundException when the uuid is unknown" {
        shouldThrow<ResourceNotFoundException> {
            effectApiService.deleteEffect(UUID.randomUUID().toString())
        }
    }

    "deleteEffect compacts remaining strip layers when a middle effect is removed" {
        val client = createLedStripClientEntity(clientRepository, "Den lights", "192.168.50.113", 110, 111)
        val strip = saveLedStrip(stripRepository, client, "Strip A", 60, PiClientPin.D21.pinName, 80)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreate(strip, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreate(strip, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreate(strip, "Z", defaultNrSettings))

        effectRepository.findByUuid(x).get().layer shouldBe 0
        effectRepository.findByUuid(y).get().layer shouldBe 1
        effectRepository.findByUuid(z).get().layer shouldBe 2

        effectApiService.deleteEffect(y)
        effectRepository.findByUuid(x).get().layer shouldBe 0
        effectRepository.findByUuid(z).get().layer shouldBe 1
    }

    "deleteEffect compacts remaining pool layers when a middle effect is removed" {
        val (pool, _) = createPool("Delete Pool", "192.168.50.144", 144, 145)
        val defaultNrSettings = objectToMap(objectMapper, NightriderColorFillEffectSettings())

        val x = effectApiService.createEffect(makeCreateForPool(pool, "X", defaultNrSettings))
        val y = effectApiService.createEffect(makeCreateForPool(pool, "Y", defaultNrSettings))
        val z = effectApiService.createEffect(makeCreateForPool(pool, "Z", defaultNrSettings))

        effectRepository.findByUuid(x).get().layer shouldBe 0
        effectRepository.findByUuid(y).get().layer shouldBe 1
        effectRepository.findByUuid(z).get().layer shouldBe 2

        effectApiService.deleteEffect(y)
        effectRepository.findByUuid(x).get().layer shouldBe 0
        effectRepository.findByUuid(z).get().layer shouldBe 1
    }

    // ---------------------------------------------------------------------
    // Effect settings
    // ---------------------------------------------------------------------

    "Effect settings preserve skipFramesIfBlank through create and patch" {
        val createBody = CreateEffectSettingsRequest(
            type = LightEffectType.NIGHTRIDER_COLOR_FILL.displayName,
            name = "Skip blank off",
            settings = objectToMap(objectMapper, NightriderColorFillEffectSettings()),
            isDefault = false,
            skipFramesIfBlank = false,
        )
        val settingsUuid = effectApiService.createEffectSettings(createBody)
        effectApiService.getEffectSettings(settingsUuid).skipFramesIfBlank shouldBe false

        effectApiService.updateEffectSettings(
            settingsUuid,
            UpdateEffectSettingsRequest(name = null, settings = null, isDefault = null, skipFramesIfBlank = true),
        )
        effectApiService.getEffectSettings(settingsUuid).skipFramesIfBlank shouldBe true
    }
})
