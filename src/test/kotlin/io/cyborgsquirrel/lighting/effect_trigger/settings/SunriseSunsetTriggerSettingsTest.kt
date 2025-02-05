package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration

@MicronautTest(startApplication = false, transactional = false)
class SunriseSunsetTriggerSettingsTest(private val objectMapper: ObjectMapper) : StringSpec({

    val sunriseSunsetTriggerSettings =
        SunriseSunsetTriggerSettings(SunriseSunsetOption.Sunset, Duration.ofHours(4), 120, TriggerType.StartEffect)
    val sunriseSunsetTriggerSettingsJson =
        "{\"activationDuration\":14400000000000,\"maxActivations\":120,\"triggerType\":\"StartEffect\",\"sunriseSunsetOption\":\"Sunset\"}"

    "Serialize to json" {
        val json = objectMapper.writeValueAsString(sunriseSunsetTriggerSettings)
        json shouldBe sunriseSunsetTriggerSettingsJson
    }

    "Serialize from json" {
        val settings =
            objectMapper.readValue(sunriseSunsetTriggerSettingsJson, SunriseSunsetTriggerSettings::class.java)
        settings.sunriseSunsetOption shouldBe sunriseSunsetTriggerSettings.sunriseSunsetOption
        settings.maxActivations shouldBe sunriseSunsetTriggerSettings.maxActivations
        settings.triggerType shouldBe sunriseSunsetTriggerSettings.triggerType
        settings.activationDuration shouldBe sunriseSunsetTriggerSettings.activationDuration
    }
})