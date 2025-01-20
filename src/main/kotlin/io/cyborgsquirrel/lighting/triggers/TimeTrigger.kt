package io.cyborgsquirrel.lighting.triggers

import io.cyborgsquirrel.lighting.enums.TriggerType
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelper
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class TimeTrigger(private val triggerTime: LocalTime, private val timeHelper: TimeHelper, triggerType: TriggerType) :
    LightEffectTrigger(triggerType) {

    override fun init() {
        // Do nothing - intentionally left blank
    }

    override fun lastActivation(): Optional<LocalDateTime> {
        val now = timeHelper.now()
        val currentTime = now.toLocalTime()

        if (currentTime.isAfter(triggerTime)) {
            val triggerDateTime = LocalDateTime.of(now.toLocalDate(), triggerTime)
            return Optional.of(triggerDateTime)
        }

        return Optional.empty()
    }

    override fun triggerThreshold(): Duration {
        return Duration.ofMinutes(5)
    }
}