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
            effectListRef.set(updated)
            updated
        }
        sink.tryEmitNext(snapshot)
    }

    override fun removeEffect(lightEffect: ActiveLightEffect) {
        val snapshot: List<ActiveLightEffect> = writeLock.withLock {
            logger.info("Removing light effect $lightEffect")
            val updated = effectListRef.get() - lightEffect
            effectListRef.set(updated)
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
        effectListRef.get().filter { it.strip.uuid == stripUuid }

    override fun getAllEffects(): List<ActiveLightEffect> = effectListRef.get()

    override fun removeAllEffects() {
        writeLock.withLock {
            effectListRef.set(emptyList())
        }
        sink.tryEmitNext(emptyList())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRegistryImpl::class.java)
    }
}
