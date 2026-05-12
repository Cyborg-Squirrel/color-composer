package io.cyborgsquirrel.lighting.effects.schemas

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class EffectSettingsSchemaBuilderTest : StringSpec({

    "effectName is set on the built schema" {
        EffectSettingsSchemaBuilder("MyEffect").integer("key").build().effectName shouldBe "MyEffect"
    }

    "correct field type and key are produced for each field type" {
        data class Case(val expectedType: EffectSettingsType, val build: () -> EffectSettingsSchemaField)

        listOf(
            Case(EffectSettingsType.Integer) { EffectSettingsSchemaBuilder("e").integer("key").build().fields.first() },
            Case(EffectSettingsType.Number) { EffectSettingsSchemaBuilder("e").number("key").build().fields.first() },
            Case(EffectSettingsType.String) { EffectSettingsSchemaBuilder("e").string("key").build().fields.first() },
            Case(EffectSettingsType.Boolean) { EffectSettingsSchemaBuilder("e").boolean("key").build().fields.first() },
            Case(EffectSettingsType.RgbColor) { EffectSettingsSchemaBuilder("e").color("key").build().fields.first() },
        ).forEach { (expectedType, build) ->
            val field = build()
            field.key shouldBe "key"
            field.type shouldBe expectedType
            field.validators shouldBe emptyList()
        }
    }

    "validators are attached to their field" {
        data class Case(val expectedValidators: List<EffectSettingsValidator>, val build: () -> EffectSettingsSchemaField)

        listOf(
            Case(listOf(EffectSettingsValidator.Min(1.0))) {
                EffectSettingsSchemaBuilder("e").integer("key").min(1.0).build().fields.first()
            },
            Case(listOf(EffectSettingsValidator.Max(10.0))) {
                EffectSettingsSchemaBuilder("e").number("key").max(10.0).build().fields.first()
            },
            Case(listOf(EffectSettingsValidator.Min(0.0), EffectSettingsValidator.Max(100.0))) {
                EffectSettingsSchemaBuilder("e").integer("key").min(0.0).max(100.0).build().fields.first()
            },
            Case(listOf(EffectSettingsValidator.Options(listOf("Linear", "Logarithmic")))) {
                EffectSettingsSchemaBuilder("e").string("key").options(listOf("Linear", "Logarithmic")).build().fields.first()
            },
        ).forEach { (expectedValidators, build) ->
            build().validators shouldContainExactly expectedValidators
        }
    }

    "validators only apply to their own field" {
        val schema = EffectSettingsSchemaBuilder("e")
            .integer("a").min(1.0)
            .integer("b").max(10.0)
            .build()
        schema.fields[0].validators shouldContainExactly listOf(EffectSettingsValidator.Min(1.0))
        schema.fields[1].validators shouldContainExactly listOf(EffectSettingsValidator.Max(10.0))
    }

    "throws when duplicate validators are provided" {
        listOf(
            { EffectSettingsSchemaBuilder("e").integer("key").min(1.0).min(2.0).build() },
            { EffectSettingsSchemaBuilder("e").number("key").max(5.0).max(10.0).build() },
            { EffectSettingsSchemaBuilder("e").string("key").options(listOf("a")).options(listOf("b")).build() },
        ).forEach { build ->
            shouldThrow<Exception> { build() }
        }
    }

    "throws when min is greater than or equal to max" {
        listOf(
            5.0 to 5.0,
            5.0 to 4.0,
            -1.0 to -2.0,
        ).forEach { (min, max) ->
            shouldThrow<Exception> {
                EffectSettingsSchemaBuilder("e").integer("key").min(min).max(max).build()
            }
        }
    }

    "does not throw when min is less than max" {
        listOf(
            0.0 to 1.0,
            -10.0 to 10.0,
            1.0 to 100.0,
        ).forEach { (min, max) ->
            EffectSettingsSchemaBuilder("e").integer("key").min(min).max(max).build()
        }
    }
})
