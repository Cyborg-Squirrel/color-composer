package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsType
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsValidator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class EffectApiServiceTest : StringSpec({

    val service = EffectApiService(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk())

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
        data class Case(val effectName: String, val expectedFields: List<Pair<String, EffectSettingsType>>)

        val schemas = service.getAllSchemas()

        listOf(
            Case(LightEffectType.SPECTRUM.displayName, listOf(
                "colorPixelWidth"  to EffectSettingsType.Integer,
                "animated"         to EffectSettingsType.Boolean,
                "updatesPerSecond" to EffectSettingsType.Integer,
            )),
            Case(LightEffectType.NIGHTRIDER_COLOR_FILL.displayName, listOf(
                "wrap"              to EffectSettingsType.Boolean,
                "updatesPerSecond"  to EffectSettingsType.Integer,
                "brightnessScaling" to EffectSettingsType.Number,
            )),
            Case(LightEffectType.NIGHTRIDER_COMET.displayName, listOf(
                "trailLength"      to EffectSettingsType.Integer,
                "trailFadeCurve"   to EffectSettingsType.String,
                "wrap"             to EffectSettingsType.Boolean,
                "updatesPerSecond" to EffectSettingsType.Integer,
            )),
            Case(LightEffectType.FLAME.displayName, listOf(
                "cooling"          to EffectSettingsType.Integer,
                "sparking"         to EffectSettingsType.Integer,
                "sparks"           to EffectSettingsType.Integer,
                "sparkHeight"      to EffectSettingsType.Integer,
                "updatesPerSecond" to EffectSettingsType.Integer,
            )),
            Case(LightEffectType.BOUNCING_BALL.displayName, listOf(
                "startingHeightPercent" to EffectSettingsType.Integer,
                "maxHeightPercent"      to EffectSettingsType.Integer,
                "speed"                 to EffectSettingsType.Number,
                "gravity"               to EffectSettingsType.Number,
                "minimumSpeed"          to EffectSettingsType.Number,
            )),
            Case(LightEffectType.WAVE.displayName, listOf(
                "startPoint"       to EffectSettingsType.Integer,
                "waveLength"       to EffectSettingsType.Integer,
                "repeat"           to EffectSettingsType.Boolean,
                "updatesPerSecond" to EffectSettingsType.Integer,
            )),
            Case(LightEffectType.MARQUEE.displayName, listOf(
                "dotLength"        to EffectSettingsType.Integer,
                "spaceBetweenDots" to EffectSettingsType.Integer,
                "updatesPerSecond" to EffectSettingsType.Integer,
            )),
            Case(LightEffectType.SPARKLE.displayName, listOf(
                "numDots"          to EffectSettingsType.Integer,
                "fadeInMillisMax"   to EffectSettingsType.Integer,
                "fadeInMillisMin"   to EffectSettingsType.Integer,
                "fadeOutMillisMax"  to EffectSettingsType.Integer,
                "fadeOutMillisMin"  to EffectSettingsType.Integer,
                "updatesPerSecond" to EffectSettingsType.Integer,
            )),
        ).forEach { (effectName, expectedFields) ->
            val schema = schemas.first { it.effectName == effectName }
            schema.fields.map { it.key to it.type } shouldBe expectedFields
        }
    }

    "fields with range validators have correct min and max" {
        data class Case(val effectName: String, val fieldKey: String, val min: Double?, val max: Double?)

        val schemas = service.getAllSchemas()

        listOf(
            Case(LightEffectType.NIGHTRIDER_COLOR_FILL.displayName, "brightnessScaling", 0.0, 1.0),
            Case(LightEffectType.FLAME.displayName,           "sparking",          0.0, 255.0),
            Case(LightEffectType.BOUNCING_BALL.displayName,          "startingHeightPercent", 0.0, 100.0),
        ).forEach { (effectName, fieldKey, expectedMin, expectedMax) ->
            val field = schemas.first { it.effectName == effectName }.fields.first { it.key == fieldKey }
            field.validators.filterIsInstance<EffectSettingsValidator.Min>().firstOrNull()?.value shouldBe expectedMin
            field.validators.filterIsInstance<EffectSettingsValidator.Max>().firstOrNull()?.value shouldBe expectedMax
        }
    }

    "trailFadeCurve field has options for all FadeCurve values" {
        val cometSchema = service.getAllSchemas().first { it.effectName == LightEffectType.NIGHTRIDER_COMET.displayName }
        val field = cometSchema.fields.first { it.key == "trailFadeCurve" }
        val options = field.validators.filterIsInstance<EffectSettingsValidator.Options>().first()
        options.values shouldBe listOf("Linear", "Logarithmic")
    }
})
