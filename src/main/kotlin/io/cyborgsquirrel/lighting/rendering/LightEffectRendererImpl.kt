package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.LedStripGroupModel
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.power_limits.PowerLimiterService
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
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

    /**
     * Renders all light effects for the specified LED strip [lightUuid].
     *
     * First all light effects in the Playing state for the [lightUuid] are rendered. Then the rendered frame data is
     * put through the filters if any are configured. If the skip blank frames configuration is set, frames will be
     * skipped if all effects end up producing blank frame data. Last, the light effects are layered on top of each other.
     * The layering algorithm may just use one effect's colors, or mix them, depending on the blend mode.
     *
     * The [sequenceNumber] is used in the case of LED strip groups, where multiple jobs rendering to multiple
     * clients will request the same color data. The first job to call this method renders the frame, the subsequent
     * callers read the buffer if the [sequenceNumber] matches.
     *
     * Returns an optional RGB data frame. If no effects are configured, or no effects are playing then the optional
     * will be empty.
     */
    override fun renderFrame(lightUuid: String, sequenceNumber: Short): Optional<RenderedFrameModel> {
        val activeEffects = effectRepository.getAllEffectsForStrip(lightUuid)
            .filter { it.status == LightEffectStatus.Playing || it.status == LightEffectStatus.Paused }
            .sortedBy { it.priority }
        if (activeEffects.isEmpty()) {
            // No active effects for the specified LED strip
            return Optional.empty()
        }

        val allEffectsRgbData = mutableListOf<List<RgbColor>>()
        for (activeEffect in activeEffects) {
            when (activeEffect.strip) {
                is LedStripModel -> {
                    logger.debug("Rendering effect {}", activeEffect)
                    var rgbData = if (activeEffect.status == LightEffectStatus.Playing) {
                        activeEffect.effect.getNextStep()
                    } else {
                        activeEffect.effect.getBuffer()
                    }
                    logger.debug("Rendered effect {}", activeEffect)
                    for (filter in activeEffect.filters) {
                        logger.debug("Applying filter ${filter.uuid}")
                        rgbData = filter.apply(rgbData)
                        logger.debug("Applied filter ${filter.uuid}")
                    }

                    if (activeEffect.skipFramesIfBlank && activeEffect.status == LightEffectStatus.Playing) {
                        var allBlank = true
                        while (allBlank) {
                            for (data in rgbData) {
                                allBlank = allBlank && data == RgbColor.Blank
                            }

                            if (allBlank) {
                                logger.debug("All frames are blank and effect is set to skip blank frames")
                                logger.debug(
                                    "Rendering next frame for effect {} {}",
                                    activeEffect.effect,
                                    activeEffect.effectUuid
                                )
                                rgbData = activeEffect.effect.getNextStep()
                                logger.debug(
                                    "Rendered next frame for effect {} {}",
                                    activeEffect.effect,
                                    activeEffect.effectUuid
                                )
                                for (filter in activeEffect.filters) {
                                    rgbData = filter.apply(rgbData)
                                }
                            }
                        }
                    }

                    allEffectsRgbData.add(rgbData)
                }

                is LedStripGroupModel -> {
                    // TODO strip group rendering
                }
            }
        }

        // If there are multiple effects, layer the RGB output on top of each other.
        val renderedRgbData = mutableListOf<RgbColor>()
        val stripLength = activeEffects.first().strip.getLength()
        val blendMode = activeEffects.first().strip.getBlendMode()
        for (i in 0..<stripLength) {
            when (blendMode) {
                BlendMode.Additive -> {
                    var didAdd = false
                    for (j in allEffectsRgbData.indices) {
                        val rgbColor = allEffectsRgbData[j][i]
                        if (!rgbColor.isBlank()) {
                            if (didAdd) {
                                renderedRgbData[i] += rgbColor
                            } else {
                                didAdd = true
                                renderedRgbData.add(rgbColor)
                            }
                        }
                    }

                    if (!didAdd) {
                        renderedRgbData.add(RgbColor.Blank)
                    }
                }

                BlendMode.Average -> TODO()
                BlendMode.Layer -> TODO()
            }
        }

        val powerLimitedRenderedRgbData = powerLimiterService.applyLimit(renderedRgbData, lightUuid)
        val effectStatuses = activeEffects.map { it.status }.toSet()
        val allEffectsPaused = effectStatuses.size == 1 && effectStatuses.first() == LightEffectStatus.Paused
        // TODO rendered RGB list layering, sequence number assignment to frames, render frame groups
        return Optional.of(RenderedFrameModel(0, lightUuid, powerLimitedRenderedRgbData, -1, allEffectsPaused))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRendererImpl::class.java)
    }
}