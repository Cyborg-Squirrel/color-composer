package io.cyborgsquirrel.engine.rendering

import io.cyborgsquirrel.engine.effects.ActiveLightEffect
import io.cyborgsquirrel.engine.enums.BlendMode
import io.cyborgsquirrel.engine.enums.LightEffectStatus
import io.cyborgsquirrel.engine.rendering.frame.BlankFrameModel
import io.cyborgsquirrel.engine.rendering.frame.RenderedFrame
import io.cyborgsquirrel.engine.rendering.frame.RenderedFrameModel
import io.cyborgsquirrel.engine.settings.LedStripRenderSettings
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.model.strip.LedStripGroupModel
import io.cyborgsquirrel.model.strip.LedStripModel
import jakarta.inject.Singleton

@Singleton
class LedStripRendererImpl : LedStripRenderer {

    // LED strip groups get rendered if the provided LED strip uuid
    // is a member of the group. To avoid re-rendering effects, buffer
    // the frame for other LED strips.
    private var stripGroupFrameBuffer = mutableListOf<RenderedFrameModel>()

    private var effectList = mutableListOf<ActiveLightEffect>()

    private var ledStripRenderSettings = mutableListOf<LedStripRenderSettings>()

    override fun addEffect(lightEffect: ActiveLightEffect) {
        effectList.add(lightEffect)
    }

    override fun getActiveEffects(): List<ActiveLightEffect> {
        return effectList.filter { it.status == LightEffectStatus.Active }
    }

    override fun renderFrame(lightUuid: String, sequenceNumber: Short): RenderedFrame {
        val activeEffects = getActiveEffects()
            .filter { it.strip.getUuid() == lightUuid }
            .sortedBy { it.priority }
        if (activeEffects.isEmpty()) {
            // No active effects for the specified LED strip
            return BlankFrameModel(lightUuid)
        }

        val renderedEffectRgbData = mutableListOf<List<RgbColor>>()
        for (activeEffect in activeEffects) {
            when (activeEffect.strip) {
                is LedStripModel -> renderedEffectRgbData.add(activeEffect.effect.getNextStep())
                is LedStripGroupModel -> {
                    // TODO strip group rendering
                }

                else -> {}
            }
        }

        // TODO rendered RGB list layering, sequence number assignment to frames, render frame groups
        return RenderedFrameModel(0, lightUuid, renderedEffectRgbData.first(), -1)
    }

    override fun getRenderFps(effectInstanceUuid: String) {
        TODO("Implement this")
    }

    override fun setRenderFps(effectInstanceUuid: String, fps: Int) {
        TODO("Implement this")
    }

    override fun setBlendMode(lightUuid: String, blendMode: BlendMode) {
        ledStripRenderSettings.filter { it.ledStrip.getUuid() == lightUuid }
            .forEach { it.blendMode = blendMode }
    }

    override fun pauseEffect(lightUuid: String, effectUuid: String) {
        effectList.filter { it.strip.getUuid() == lightUuid && it.effect.uuid == effectUuid }
            .forEach { it.status = LightEffectStatus.Paused }
    }
}