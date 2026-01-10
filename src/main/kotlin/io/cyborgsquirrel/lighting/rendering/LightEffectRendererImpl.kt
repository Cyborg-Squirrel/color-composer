package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.isActive
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.lighting.rendering.cache.ClientSequenceTracker
import io.cyborgsquirrel.lighting.rendering.cache.StripPoolFrameCache
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel
import io.cyborgsquirrel.lighting.rendering.post_processing.EffectsBlender
import io.cyborgsquirrel.lighting.rendering.post_processing.FrameSegmentationHelper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Singleton
class LightEffectRendererImpl(
    private val effectRepository: ActiveLightEffectService,
) : LightEffectRenderer {

    private val cache = StripPoolFrameCache()
    private val tracker = ClientSequenceTracker()
    private val segmentationHelper = FrameSegmentationHelper()
    private val blender = EffectsBlender()

    /**
     * Renders all light effects for the specified LED strips [strips].
     */
    override fun renderFrames(strips: List<LedStripModel>, clientUuid: String): List<RenderedFrameSegmentModel> {
        val frameList = mutableListOf<RenderedFrameSegmentModel>()
        for (strip in strips) {
            when (strip) {
                is LedStripPoolModel -> {
                    val sequenceNumber = tracker.getSequenceNumber(clientUuid, strip.uuid)
                    val cachedFrame = checkCache(strip, sequenceNumber)
                    val renderedFrame = if (cachedFrame != null) {
                        cachedFrame
                    } else {
                        val renderedFrame = renderFrame(strip)
                        if (renderedFrame != null) {
                            renderedFrame.sequenceNumber = cache.getSequenceNumber(strip.uuid)
                            cache.addFrameToCache(renderedFrame)
                        }

                        renderedFrame
                    }

                    if (renderedFrame != null) {
                        val frameSegments = segmentationHelper.segmentFrame(strips, clientUuid, renderedFrame)
                        frameList.addAll(frameSegments)
                    }
                }

                is SingleLedStripModel -> {
                    val renderedFrame = renderFrame(strip)
                    if (renderedFrame != null) {
                        frameList.add(
                            RenderedFrameSegmentModel(
                                strip,
                                renderedFrame.sequenceNumber,
                                renderedFrame.frameData
                            )
                        )
                    }
                }
            }
        }

        return frameList
    }

    private fun checkCache(strip: LedStripModel, sequenceNumber: Short): RenderedFrameModel? {
        if (strip is LedStripPoolModel) {
            val frame = cache.getFrameFromCache(strip.uuid, sequenceNumber)
            // Only return frame if we get a cache hit
            if (frame != null) {
                return frame
            }
        }

        return null
    }

    private fun renderFrame(strip: LedStripModel): RenderedFrameModel? {
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
        val activeEffectsByPriority = activeEffects.sortedBy { it.priority }
        for (activeEffect in activeEffectsByPriority) {
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
        val renderedRgbData = blender.blendEffects(strip, allEffectsRgbData)
        return RenderedFrameModel(strip, renderedRgbData)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRendererImpl::class.java)
    }
}