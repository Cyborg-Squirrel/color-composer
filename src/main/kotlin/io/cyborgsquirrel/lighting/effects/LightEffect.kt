package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.LightEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper

sealed class LightEffect(
    open val settings: LightEffectSettings,
    open val palette: ColorPalette?,
    protected val timeHelper: TimeHelper,
) {

    protected var lastUpdatedMillis = 0L

    protected fun isUpdateDue(updatesPerSecond: Int): Boolean {
        val nowMillis = timeHelper.millisSinceEpoch()
        if ((nowMillis - lastUpdatedMillis) > 1000L / updatesPerSecond) {
            lastUpdatedMillis = nowMillis
            return true
        }
        return false
    }

    abstract fun getNextStep(): List<RgbColor>

    abstract fun getBuffer(): List<RgbColor>

    abstract fun getIterations(): Int

    abstract fun updatePalette(palette: ColorPalette)
}