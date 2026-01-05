package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.isActive
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

@Singleton
class LightEffectRendererImpl(
    private val effectRepository: ActiveLightEffectService,
) : LightEffectRenderer {

    /**
     * Frame buffer. Used to avoid re-rendering effects for LED strip pools.
     */
    private var stripPoolFrames = mutableListOf<RenderedFrameModel>()

    /**
     * Renders all light effects for the specified LED strip [strip].
     *
     * First all light effects in the Playing state for the [strip] are rendered. Then the rendered frame data is
     * put through the filters if any are configured. If the skip blank frames configuration is set, frames will be
     * skipped if all effects end up producing blank frame data. Last, the light effects are layered on top of each other.
     * The layering algorithm may just use one effect's colors, or mix them, depending on the blend mode.
     *
     * The [sequenceNumber] is used in the case of LED strip pools, where multiple jobs rendering to multiple
     * clients will request the same color data. The first job to call this method renders the frame, the subsequent
     * callers read the buffer if the [sequenceNumber] matches.
     *
     * Returns an optional RGB data frame. If no effects are configured, or no effects are playing then the optional
     * will be empty.
     */
    override fun renderFrame(
        strip: LedStripModel, sequenceNumber: Short
    ): Optional<RenderedFrameModel> {
        val stripUuid = strip.uuid
        val isPool = strip is LedStripPoolModel
        if (isPool) {
            val matchingFramesForStrip = stripPoolFrames.filter { it.stripUuid == stripUuid }
            if (matchingFramesForStrip.isNotEmpty()) {
                val poolFrameSequenceNumbers = mutableSetOf<Short>()
                if (matchingFramesForStrip.size > 2) {
                    val sequenceNumbersSorted = matchingFramesForStrip.map { it.sequenceNumber }.sortedDescending()
                    val sequenceNumbersToPrune = mutableSetOf<Short>()
                    poolFrameSequenceNumbers.addAll(sequenceNumbersSorted - sequenceNumbersToPrune)
                    for (i in 0..<sequenceNumbersSorted.size - 1) {
                        // Handle wrap scenario (1 is greater than Short.MAX_VALUE in this case)
                        if (sequenceNumbersSorted[i] - sequenceNumbersSorted[i + 1] > 1) {
                            sequenceNumbersToPrune.add(sequenceNumbersSorted[i])
                        } else if (i > 1) {
                            // i = 2 or more, we have enough buffered frames and can remove the rest
                            sequenceNumbersToPrune.add(sequenceNumbersSorted[i])
                        }
                    }

                    stripPoolFrames.removeIf { it.stripUuid == stripUuid && sequenceNumbersToPrune.contains(it.sequenceNumber) }
                } else {
                    poolFrameSequenceNumbers.addAll(matchingFramesForStrip.map { it.sequenceNumber })
                }

                // sequenceNumber 0 means the caller is making its first call. Return the latest frame.
                val frame = if (sequenceNumber <= 0) {
                    matchingFramesForStrip.maxByOrNull { it.sequenceNumber }!!
                } else if (poolFrameSequenceNumbers.contains(sequenceNumber)) {
                    matchingFramesForStrip.first { it.sequenceNumber == sequenceNumber }
                } else {
                    null
                }

                return if (frame != null) {
                    Optional.of(frame)
                } else {
                    Optional.empty()
                }
            }
        }

        // TODO caching strategy, is 10 frames enough?
        if (stripPoolFrames.size > 10) {
            stripPoolFrames.removeLast()
        }

        val activeEffects =
            effectRepository.getAllEffectsForStrip(stripUuid).filter { it.status.isActive() }.sortedBy { it.priority }
        return if (activeEffects.isEmpty()) {
            Optional.empty()
        } else {
            renderFrame(activeEffects, isPool)
        }
    }

    private fun renderFrame(activeEffects: List<ActiveLightEffect>, isPool: Boolean): Optional<RenderedFrameModel> {
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
            activeEffects.first().strip.uuid, renderedRgbData, -1, allEffectsPaused
        )

        if (isPool) {
            stripPoolFrames.add(frame)
        }

        return Optional.of(frame)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRendererImpl::class.java)
    }
}