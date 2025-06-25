package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration

@MicronautTest(startApplication = false, transactional = false)
class SunriseSunsetTriggerSettingsTest(private val objectMapper: ObjectMapper) : StringSpec({

    val timeOfDayTriggerSettings =
        TimeOfDayTriggerSettings(TimeOfDay.Sunset, Duration.ofHours(4), 120, TriggerType.StartEffect)
    val sunriseSunsetTriggerSettingsJson =
        "{\"activationDuration\":14400000,\"maxActivations\":120,\"triggerType\":\"StartEffect\",\"timeOfDay\":\"Sunset\"}"

    "Serialize to json" {
        val json = objectMapper.writeValueAsString(timeOfDayTriggerSettings)
        json shouldBe sunriseSunsetTriggerSettingsJson
    }

    "Serialize from json" {
        val settings =
            objectMapper.readValue(sunriseSunsetTriggerSettingsJson, TimeOfDayTriggerSettings::class.java)
        settings.timeOfDay shouldBe timeOfDayTriggerSettings.timeOfDay
        settings.maxActivations shouldBe timeOfDayTriggerSettings.maxActivations
        settings.triggerType shouldBe timeOfDayTriggerSettings.triggerType
        settings.activationDuration shouldBe timeOfDayTriggerSettings.activationDuration
    }
})