package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.isActive
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.rendering.cache.StripPoolFrameCache
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Singleton
class LightEffectRendererImpl(
    private val effectRepository: ActiveLightEffectService,
) : LightEffectRenderer {

    private val cache = StripPoolFrameCache()

    /**
     * Renders all light effects for the specified LED strip [strip].
     */
    override fun renderFrame(
        strip: LedStripModel, sequenceNumber: Short
    ): RenderedFrameModel? {
        if (strip is LedStripPoolModel) {
            val frame = cache.getFrameFromCache(strip.uuid, sequenceNumber)
            // Only return frame if we get a cache hit
            if (frame != null) {
                return frame
            }
        }

        val activeEffects =
            effectRepository.getAllEffectsForStrip(strip.uuid).filter { it.status.isActive() }.sortedBy { it.priority }
        return if (activeEffects.isEmpty()) {
            null
        } else {
            renderFrame(strip, activeEffects)
        }
    }

    private fun renderFrame(
        strip: LedStripModel,
        activeEffects: List<ActiveLightEffect>
    ): RenderedFrameModel {
        val allEffectsRgbData = mutableListOf<List<RgbColor>>()
        for (activeEffect in activeEffects) {
            logger.debug("Rendering effect {}", activeEffect)
            var rgbData = if (activeEffect.status == LightEffectStatus.Playing) {
                activeEffect.effect.getNextStep()
            } else {
                activeEffect.effect.getBuffer()
            }
            for (filter in activeEffect.filters) {
                logger.debug("Applying filter ${filter.uuid}")
                rgbData = filter.apply(rgbData)
            }

            if (activeEffect.skipFramesIfBlank && activeEffect.status == LightEffectStatus.Playing) {
                var allBlank = true
                while (allBlank) {
                    for (data in rgbData) {
                        allBlank = allBlank && data == RgbColor.Blank
                    }

                    if (allBlank) {
                        logger.debug(
                            "All frames are blank and effect {} is set to skip blank frames", activeEffect.effectUuid
                        )
                        rgbData = activeEffect.effect.getNextStep()
                        for (filter in activeEffect.filters) {
                            rgbData = filter.apply(rgbData)
                        }
                    }
                }
            }

            allEffectsRgbData.add(rgbData)
        }

        // If there are multiple effects, layer the RGB output on top of each other.
        val renderedRgbData = mutableListOf<RgbColor>()
        val stripLength = activeEffects.first().strip.length()
        val blendMode = activeEffects.first().strip.blendMode
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

        val effectStatuses = activeEffects.map { it.status }.toSet()
        val allEffectsPaused = effectStatuses.size == 1 && effectStatuses.first() == LightEffectStatus.Paused
        // TODO rendered RGB list layering, sequence number assignment to frames, render frame pools
        val frame = RenderedFrameModel(
            strip, renderedRgbData, cache.getSequenceNumber(strip.uuid), allEffectsPaused
        )

        if (strip is LedStripPoolModel) {
            cache.addFrameToCache(frame)
        }

        return frame
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRendererImpl::class.java)
    }
}