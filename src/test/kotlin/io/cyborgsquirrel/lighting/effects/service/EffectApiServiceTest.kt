package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsSchemaField
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsType
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsValidator
import io.cyborgsquirrel.lighting.effects.settings.*
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.mockk
import kotlin.math.max

@MicronautTest
class EffectApiServiceTest(val objectMapper: ObjectMapper) : StringSpec({

    val service = EffectApiService(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk())

    "getAllSchemas returns one schema per effect" {
        service.getAllSchemas().size shouldBe 8
    }

    "all schema fields have non-blank keys and descriptions" {
        service.getAllSchemas().flatMap { it.fields }.forEach { field ->
            field.key.isNotBlank() shouldBe true
            field.description.isNotBlank() shouldBe true
        }
    }

    "each effect schema has the correct fields" {
        data class Field(val key: String, val type: EffectSettingsType, val default: Any?)
        data class Case(val effectName: String, val expectedFields: List<Field>)

        val schemas = service.getAllSchemas()

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

        val schemas = service.getAllSchemas()

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
            service.getAllSchemas().first { it.effectName == LightEffectType.NIGHTRIDER_COMET.displayName }
        val field = cometSchema.fields.first { it.key == "trailFadeCurve" }
        val options = field.validators.filterIsInstance<EffectSettingsValidator.Options>().first()
        options.values shouldBe listOf("Linear", "Logarithmic")
    }

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

    "Verify schemas" {
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

        val schemas = service.getAllSchemas()

        cases.forEach { (effectType, settingsClass) ->
            val schema = schemas.first { it.effectName == effectType.displayName }
            val json = schema.fields.joinToString(",", "{", "}") { "\"${it.key}\":${jsonValueFor(it)}" }
            @Suppress("UNCHECKED_CAST")
            val result = objectMapper.readValue(json, settingsClass as Class<Any>)!!
            result::class.java shouldBe settingsClass
        }
    }
})
