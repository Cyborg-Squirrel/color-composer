package io.cyborgsquirrel.led_strips.enums

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@AnnotationSpec.Test
class PiClientPinTest : StringSpec({

    "Happy path" {
        var pin = PiClientPin.fromString("D10")
        pin shouldBe PiClientPin.D10
        pin = PiClientPin.fromString("D12")
        pin shouldBe PiClientPin.D12
        pin = PiClientPin.fromString("D18")
        pin shouldBe PiClientPin.D18
        pin = PiClientPin.fromString("D21")
        pin shouldBe PiClientPin.D21
    }

    "Validate" {
        var isValid = PiClientPin.isValid("D10")
        isValid shouldBe true
        isValid = PiClientPin.isValid("D12")
        isValid shouldBe true
        isValid = PiClientPin.isValid("D18")
        isValid shouldBe true
        isValid = PiClientPin.isValid("D21")
        isValid shouldBe true
        isValid = PiClientPin.isValid("10")
        isValid shouldBe false
    }

    "Invalid - throws Exception" {
        shouldThrow<Exception> { PiClientPin.fromString("D11") }
        shouldThrow<Exception> { PiClientPin.fromString("12") }
        shouldThrow<Exception> { PiClientPin.fromString("18D") }
    }
})