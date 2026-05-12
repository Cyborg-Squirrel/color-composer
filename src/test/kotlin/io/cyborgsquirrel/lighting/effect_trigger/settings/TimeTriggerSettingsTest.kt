package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.time.LocalTime

@MicronautTest(startApplication = false, transactional = false)
class TimeTriggerSettingsTest(private val objectMapper: ObjectMapper) : StringSpec({

    val timeTriggerSettings =
        TimeTrigger(LocalTime.of(18, 0), null, Duration.ofHours(4), 120, TriggerType.StartEffect)
    val timeTriggerSettingsJson =
        "{\"activationDuration\":14400000,\"maxActivations\":120,\"triggerType\":\"StartEffect\",\"metadata\":{\"majorVersion\":1,\"minorVersion\":0},\"triggerTime\":\"18:00:00\"}"

    "Serialize to json" {
        val json = objectMapper.writeValueAsString(timeTriggerSettings)
        json shouldBe timeTriggerSettingsJson
    }

    "Serialize from json" {
        val settings =
            objectMapper.readValue(timeTriggerSettingsJson, TimeTrigger::class.java)
        settings.triggerTime shouldBe timeTriggerSettings.triggerTime
        settings.maxActivations shouldBe timeTriggerSettings.maxActivations
        settings.triggerType shouldBe timeTriggerSettings.triggerType
        settings.activationDuration shouldBe timeTriggerSettings.activationDuration
        settings.metadata.majorVersion shouldBe 1
        settings.metadata.minorVersion shouldBe 0
    }
})