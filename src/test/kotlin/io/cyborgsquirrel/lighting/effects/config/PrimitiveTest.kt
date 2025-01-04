package io.cyborgsquirrel.lighting.effects.config

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

@AnnotationSpec.Test
class PrimitiveTest : StringSpec({

    "To and from string" {
        val f = Primitive.Float
        val floatString = "Float"

        Primitive.fromString(floatString) shouldBe f
        f.toString() shouldBeEqual floatString
    }
})