package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.util.time.TimeHelper
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class TimeTrigger(private val timeHelper: TimeHelper, settings: TimeTriggerSettings, uuid: String, effectUuid: String) :
    LightEffectTrigger(settings, uuid, effectUuid) {

    private var sequenceNumber = 0
    private var lastActivation: LocalDateTime? = null

    override fun lastActivation(): Optional<TriggerActivation> {
        val now = timeHelper.now()
        val currentTime = now.toLocalTime()
        val triggerTime = getTriggerTime()

        if (currentTime.isAfter(triggerTime) && now.toLocalDate() != lastActivation?.toLocalDate()) {
            sequenceNumber++
            lastActivation = LocalDateTime.of(now.toLocalDate(), triggerTime)
        }

        return if (lastActivation == null) Optional.empty() else Optional.of(
            TriggerActivation(
                lastActivation!!,
                settings,
                sequenceNumber,
            )
        )
    }

    private fun getTriggerTime(): LocalTime {
        return (settings as TimeTriggerSettings).triggerTime
    }
}