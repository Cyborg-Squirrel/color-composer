package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A service for memory storage of [ActiveLightEffect]s.
 */
@Singleton
class LightEffectRegistryImpl : LightEffectRegistry {
    private val sink: Sinks.Many<List<ActiveLightEffect>> = Sinks.many().multicast().onBackpressureBuffer()
    private val effectListRef = AtomicReference(listOf<ActiveLightEffect>())

    // Effects grouped by strip uuid, rebuilt only when the effect set changes. This keeps the per-frame render path
    // off the global effect list, and the per-strip list instance is stable between changes so the renderer can use
    // its identity to detect that a strip's effects are unchanged.
    private val effectsByStripRef = AtomicReference(mapOf<String, List<ActiveLightEffect>>())
    private val writeLock = ReentrantLock()

    override val updates: Flux<List<ActiveLightEffect>> = sink.asFlux()

    override fun addOrUpdateEffect(lightEffect: ActiveLightEffect) {
        val snapshot: List<ActiveLightEffect> = writeLock.withLock {
            val current = effectListRef.get()
            val updated = if (current.none { it.effectUuid == lightEffect.effectUuid }) {
                logger.info("New light effect $lightEffect")
                current + lightEffect
            } else {
                logger.info("Updating light effect $lightEffect")
                current.map { if (it.effectUuid == lightEffect.effectUuid) lightEffect else it }
            }
            setEffects(updated)
            updated
        }
        sink.tryEmitNext(snapshot)
    }

    override fun removeEffect(lightEffect: ActiveLightEffect) {
        val snapshot: List<ActiveLightEffect> = writeLock.withLock {
            logger.info("Removing light effect $lightEffect")
            val updated = effectListRef.get() - lightEffect
            setEffects(updated)
            updated
        }
        sink.tryEmitNext(snapshot)
    }

    override fun getEffectWithUuid(uuid: String) = effectListRef.get().firstOrNull { it.effectUuid == uuid }

    override fun getEffectsForClient(clientUuid: String): List<ActiveLightEffect> =
        effectListRef.get().filter {
            when (it.strip) {
                is LedStripPoolModel -> it.strip.clientUuids().contains(clientUuid)
                is SingleLedStripModel -> it.strip.clientUuid == clientUuid
            }
        }

    override fun getAllEffectsForStrip(stripUuid: String): List<ActiveLightEffect> =
        effectsByStripRef.get()[stripUuid] ?: emptyList()

    override fun getAllEffects(): List<ActiveLightEffect> = effectListRef.get()

    override fun removeAllEffects() {
        writeLock.withLock {
            setEffects(emptyList())
        }
        sink.tryEmitNext(emptyList())
    }

    /** Must be called while holding [writeLock]. Updates the effect list and the strip index together. */
    private fun setEffects(effects: List<ActiveLightEffect>) {
        effectListRef.set(effects)
        effectsByStripRef.set(effects.groupBy { it.strip.uuid })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRegistryImpl::class.java)
    }
}
