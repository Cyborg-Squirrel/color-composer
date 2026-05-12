package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.util.time.TimeHelper
import java.time.LocalDateTime
import java.util.*

class TimeTrigger(private val timeHelper: TimeHelper, settings: TimeTriggerSettings, uuid: String, effectUuid: String) :
    LightEffectTrigger(settings, uuid, effectUuid) {

    private var activationNumber = 0
    private var lastActivation: LocalDateTime? = null

    override fun lastActivation(): Optional<TriggerActivation> {
        val now = timeHelper.now()
        val currentTime = now.toLocalTime()
        val triggerTime = getTriggerTime()
        val triggerDay = getTriggerDay()

        if (triggerDay != null) {
            val triggerDateTime = LocalDateTime.of(triggerDay, triggerTime)
            if (now.isAfter(triggerDateTime) && now.toLocalDate() != lastActivation?.toLocalDate()) {
                activationNumber++
                lastActivation = LocalDateTime.of(now.toLocalDate(), triggerTime)
            }
        } else if (currentTime.isAfter(triggerTime) && now.toLocalDate() != lastActivation?.toLocalDate()) {
            activationNumber++
            lastActivation = LocalDateTime.of(now.toLocalDate(), triggerTime)
        }

        return if (lastActivation == null) Optional.empty() else Optional.of(
            TriggerActivation(
                lastActivation!!,
                settings,
                activationNumber,
            )
        )
    }

    private fun getTriggerTime() = (settings as TimeTriggerSettings).triggerTime

    private fun getTriggerDay() = (settings as TimeTriggerSettings).triggerDay
}