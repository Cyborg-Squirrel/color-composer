package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.isActive
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

@Singleton
class LightEffectRendererImpl(
    private val effectRepository: ActiveLightEffectRegistry,
) : LightEffectRenderer {

    // LED strip pools get rendered if the provided LED strip uuid
    // is a member of the pool. To avoid re-rendering effects, buffer
    // the frame for other LED strips.
    private var stripPoolFrameBuffer = mutableListOf<RenderedFrameModel>()

    /**
     * Renders all light effects for the specified LED strips [stripUuids].
     *
     * First all light effects in the Playing state for the [stripUuids] are rendered. Then the rendered frame data is
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
    override fun renderFrames(stripUuids: List<String>, sequenceNumber: Short): List<RenderedFrameModel> {
        val effectsMap = mutableMapOf<String, List<ActiveLightEffect>>()
        for (stripUuid in stripUuids) {
            val activeEffects = effectRepository.getAllEffectsForStrip(stripUuid).filter { it.status.isActive() }
                .sortedBy { it.priority }
            if (activeEffects.isNotEmpty()) {
                effectsMap[stripUuid] = activeEffects
            }
        }

        // Nothing to render so we can return an empty list
        if (effectsMap.isEmpty()) {
            return listOf()
        }

        val renderedFrames = effectsMap.values.map {
            renderFrame(it)
        }.filter { it.isPresent }.map {
            it.get()
        }

        return renderedFrames
    }

    override fun renderFrame(
        stripUuid: String,
        sequenceNumber: Short
    ): Optional<RenderedFrameModel> {
        val activeEffects = effectRepository.getAllEffectsForStrip(stripUuid).filter { it.status.isActive() }
            .sortedBy { it.priority }
        return if (activeEffects.isEmpty()) {
            Optional.empty()
        } else {
            renderFrame(activeEffects)
        }
    }

    private fun renderFrame(activeEffects: List<ActiveLightEffect>): Optional<RenderedFrameModel> {
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
                            "All frames are blank and effect {} is set to skip blank frames",
                            activeEffect.effectUuid
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
        val stripLength = activeEffects.first().strip.length
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
        return Optional.of(
            RenderedFrameModel(
                0, activeEffects.first().strip.uuid, renderedRgbData, -1, allEffectsPaused
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRendererImpl::class.java)
    }
}