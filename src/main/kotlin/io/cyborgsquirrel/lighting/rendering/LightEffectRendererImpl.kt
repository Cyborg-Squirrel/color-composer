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
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
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

    // Active effects grouped by strip uuid, filtered to in-use and sorted by layer. Maintained off the render path:
    // seeded at construction and rebuilt only when the registry publishes an effect-set change, so per-frame
    // rendering is just a map lookup. Held in an AtomicReference and swapped wholesale so readers always see a
    // consistent snapshot.
    private val activeEffectsByStrip = AtomicReference<Map<String, List<ActiveLightEffect>>>(emptyMap())

    // Subscribe before seeding so a change racing construction is still applied (the seed below then reads the
    // same-or-newer registry state). The rebuild is guarded so a thrown exception can't terminate the subscription
    // and leave the cache permanently stale.
    private val updatesSubscription: Disposable = effectRepository.updates.subscribe(
        { snapshot -> runCatching { rebuildCache(snapshot) }.onFailure { logger.error("Failed to rebuild effect cache", it) } },
        { logger.error("Effect updates stream terminated; effect cache will no longer refresh", it) },
    )

    init {
        // The multicast updates Flux only delivers future changes, so seed the cache with the current effect set.
        rebuildCache(effectRepository.getAllEffects())
    }

    /**
     * Renders all light effects for the specified LED strips [strips].
     */
    override fun renderFrames(
        strips: List<LedStripModel>,
        clientUuid: String,
    ): List<RenderedFrameSegmentModel> {
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
        val activeEffects = activeEffectsForStrip(strip.uuid)
        return if (activeEffects.isEmpty()) {
            null
        } else {
            renderFrame(strip, activeEffects)
        }
    }

    private fun activeEffectsForStrip(stripUuid: String): List<ActiveLightEffect> =
        activeEffectsByStrip.get()[stripUuid] ?: emptyList()

    /** Rebuilds the per-strip active-effect index from a full effect snapshot. Called off the render path. */
    private fun rebuildCache(allEffects: List<ActiveLightEffect>) {
        activeEffectsByStrip.set(
            allEffects
                .filter { it.status.isInUse() }
                .groupBy { it.strip.uuid }
                .mapValues { (_, effects) -> effects.sortedBy { it.layer } }
        )
    }

    @PreDestroy
    fun close() {
        updatesSubscription.dispose()
    }

    private fun renderFrame(
        strip: LedStripModel,
        activeEffects: List<ActiveLightEffect>,
    ): RenderedFrameModel {
        val stripLength = strip.length()
        val allEffectsRgbData = ArrayList<List<RgbColor>>(activeEffects.size)
        for (activeEffect in activeEffects) {
            logger.debug("Rendering effect {}", activeEffect)
            val playing = activeEffect.status == LightEffectStatus.Playing
            var rgbData = if (playing) activeEffect.effect.getNextStep() else activeEffect.effect.getBuffer()

            for (filter in activeEffect.filters) {
                logger.debug("Applying filter {}", filter.uuid)
                rgbData = filter.apply(rgbData)
            }

            if (activeEffect.skipFramesIfBlank && playing) {
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

            if (rgbData.size > stripLength) {
                logger.warn(
                    "Effect {} output {} LEDs, truncating to strip length {}",
                    activeEffect.effectUuid, rgbData.size, stripLength
                )
                rgbData = rgbData.take(stripLength)
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
