package io.cyborgsquirrel.lighting.effects.helpers

import io.cyborgsquirrel.util.time.TimeHelper

class EffectUpdateTickChecker(private val timeHelper: TimeHelper) {

    var lastUpdatedAtMillis: Long = 0

    fun isUpdateDue(updatesPerSecond: Int): Boolean {
        val nowMillis = timeHelper.millisSinceEpoch()
        return (nowMillis - lastUpdatedAtMillis) > 1000L / updatesPerSecond
    }

    fun onUpdate(updatedAtMillis: Long) {
        lastUpdatedAtMillis = updatedAtMillis
    }
}