package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.LightEffectRegistry
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.isInUse
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Singleton
class LightEffectRendererImpl(
    private val effectRepository: LightEffectRegistry,
) : LightEffectRenderer {

    // Per-pool locks so independent pools render concurrently; only jobs sharing a pool serialize against each other.
    private val poolLocks = ConcurrentHashMap<String, ReentrantLock>()
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
                    /// Per-pool lock because strip pools can involve multiple clients and thus multiple job threads
                    /// jobs should either get the latest frame or the frame cached from the other job's render sequence
                    val poolLock = poolLocks.getOrPut(strip.uuid) { ReentrantLock() }
                    poolLock.withLock {
                        val sequenceNumber = tracker.getSequenceNumber(clientUuid, strip.uuid)
                        tracker.setSequenceNumber(clientUuid, strip.uuid, (sequenceNumber + 1).toShort())
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
        val effectsForStrip = effectRepository.getAllEffectsForStrip(strip.uuid)
        val activeEffects = effectsForStrip.filter { it.status.isInUse() }.sortedBy { it.layer }
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
        val allEffectsRgbData = ArrayList<List<RgbColor>>(activeEffects.size)
        for (activeEffect in activeEffects) {
            logger.debug("Rendering effect {}", activeEffect)
            // Advance the effect only when it is playing and due for an update; otherwise reuse its current buffer.
            // This is where per-effect throttling (updatesPerSecond) happens.
            val due = activeEffect.status == LightEffectStatus.Playing
            var rgbData = if (due) activeEffect.effect.getNextStep() else activeEffect.effect.getBuffer()

            for (filter in activeEffect.filters) {
                logger.debug("Applying filter {}", filter.uuid)
                rgbData = filter.apply(rgbData)
            }

            if (activeEffect.skipFramesIfBlank && due) {
                var skipped = 0
                while (skipped < MAX_BLANK_FRAME_SKIPS && rgbData.all { it.isBlank() }) {
                    logger.debug(
                        "All frames are blank and effect {} is set to skip blank frames", activeEffect.effectUuid
                    )
                    rgbData = activeEffect.effect.getNextStep()
                    for (filter in activeEffect.filters) {
                        rgbData = filter.apply(rgbData)
                    }
                    skipped++
                }
            }

            allEffectsRgbData.add(rgbData)
        }

        // If there are multiple effects, layer the RGB output on top of each other.
        val renderedRgbData = blender.blendEffects(strip, allEffectsRgbData)
        return RenderedFrameModel(strip, renderedRgbData)
    }

    companion object {
        // Safety bound so an effect that always renders blank can't spin the render loop forever.
        // 4 seconds at 30 fps = 120
        private const val MAX_BLANK_FRAME_SKIPS = 120
        private val logger = LoggerFactory.getLogger(LightEffectRendererImpl::class.java)
    }
}
