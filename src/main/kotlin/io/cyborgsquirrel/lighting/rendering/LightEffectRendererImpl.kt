package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.repository.ActiveLightEffectRepository
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.rendering.frame.BlankFrameModel
import io.cyborgsquirrel.lighting.rendering.frame.RenderedFrame
import io.cyborgsquirrel.lighting.rendering.frame.RenderedFrameModel
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.model.strip.LedStripGroupModel
import io.cyborgsquirrel.model.strip.LedStripModel
import jakarta.inject.Singleton

@Singleton
class LightEffectRendererImpl(private val effectRepository: ActiveLightEffectRepository) : LightEffectRenderer {

    // LED strip groups get rendered if the provided LED strip uuid
    // is a member of the group. To avoid re-rendering effects, buffer
    // the frame for other LED strips.
    private var stripGroupFrameBuffer = mutableListOf<RenderedFrameModel>()

    override fun renderFrame(lightUuid: String, sequenceNumber: Short): RenderedFrame {
        val activeEffects =
            effectRepository.findEffectsWithStatus(LightEffectStatus.Active).filter { it.strip.getUuid() == lightUuid }
                .sortedBy { it.priority }
        if (activeEffects.isEmpty()) {
            // No active effects for the specified LED strip
            return BlankFrameModel(lightUuid)
        }

        val renderedEffectRgbData = mutableListOf<List<RgbColor>>()
        for (activeEffect in activeEffects) {
            when (activeEffect.strip) {
                is LedStripModel -> {
                    var rgbData = activeEffect.effect.getNextStep()
                    for (filter in activeEffect.filters) {
                        rgbData = filter.apply(rgbData)
                    }
                    renderedEffectRgbData.add(rgbData)
                }

                is LedStripGroupModel -> {
                    // TODO strip group rendering
                }

                else -> {}
            }
        }

        // TODO rendered RGB list layering, sequence number assignment to frames, render frame groups
        return RenderedFrameModel(0, lightUuid, renderedEffectRgbData.first(), -1)
    }
}