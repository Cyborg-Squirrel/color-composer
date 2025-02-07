package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest(startApplication = false, transactional = false)
class EffectIterationTriggerSettingsTest(private val objectMapper: ObjectMapper) : StringSpec({

    val effectIterationTriggerSettings = EffectIterationTriggerSettings(7)
    val effectIterationTriggerSettingsJson =
        "{\"activationDuration\":0,\"maxActivations\":7,\"triggerType\":\"StopEffect\"}"

    "Serialize to json" {
        val json = objectMapper.writeValueAsString(effectIterationTriggerSettings)
        json shouldBe effectIterationTriggerSettingsJson
    }

    "Serialize from json" {
        val settings =
            objectMapper.readValue(effectIterationTriggerSettingsJson, EffectIterationTriggerSettings::class.java)
        settings.maxActivations shouldBe effectIterationTriggerSettings.maxActivations
        settings.triggerType shouldBe effectIterationTriggerSettings.triggerType
        settings.activationDuration shouldBe effectIterationTriggerSettings.activationDuration
    }
})