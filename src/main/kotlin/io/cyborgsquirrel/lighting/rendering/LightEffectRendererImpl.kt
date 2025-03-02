package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.rendering.limits.PowerLimiterService
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.model.strip.LedStripGroupModel
import io.cyborgsquirrel.model.strip.LedStripModel
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

@Singleton
class LightEffectRendererImpl(
    private val effectRepository: ActiveLightEffectRegistry,
    private val powerLimiterService: PowerLimiterService,
) : LightEffectRenderer {

    // LED strip groups get rendered if the provided LED strip uuid
    // is a member of the group. To avoid re-rendering effects, buffer
    // the frame for other LED strips.
    private var stripGroupFrameBuffer = mutableListOf<RenderedFrameModel>()

    override fun renderFrame(lightUuid: String, sequenceNumber: Short): Optional<RenderedFrameModel> {
        val activeEffects =
            effectRepository.findEffectsWithStatus(LightEffectStatus.Playing).filter { it.strip.getUuid() == lightUuid }
                .sortedBy { it.priority }
        if (activeEffects.isEmpty()) {
            // No active effects for the specified LED strip
            return Optional.empty()
        }

        val renderedEffectRgbData = mutableListOf<List<RgbColor>>()
        for (activeEffect in activeEffects) {
            when (activeEffect.strip) {
                is LedStripModel -> {
                    var rgbData = activeEffect.effect.getNextStep()
                    for (filter in activeEffect.filters) {
                        rgbData = filter.apply(rgbData)
                    }

                    if (activeEffect.skipFramesIfBlank) {
                        var allBlank = true
                        while (allBlank) {
                            for (data in rgbData) {
                                allBlank = allBlank && data == RgbColor.Blank
                            }

                            if (allBlank) {
                                rgbData = activeEffect.effect.getNextStep()
                                for (filter in activeEffect.filters) {
                                    rgbData = filter.apply(rgbData)
                                }
                            }
                        }
                    }

                    rgbData = powerLimiterService.applyLimit(rgbData, activeEffect.strip.getUuid())
                    renderedEffectRgbData.add(rgbData)
                }

                is LedStripGroupModel -> {
                    // TODO strip group rendering
                }

                else -> {}
            }
        }

        // TODO rendered RGB list layering, sequence number assignment to frames, render frame groups
        return Optional.of(RenderedFrameModel(0, lightUuid, renderedEffectRgbData.first(), -1))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRendererImpl::class.java)
    }
}