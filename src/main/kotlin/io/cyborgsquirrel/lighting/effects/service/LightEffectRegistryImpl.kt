package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A service for memory storage of [ActiveLightEffect]s.
 */
@Singleton
class LightEffectRegistryImpl : LightEffectRegistry {
    private val listeners = CopyOnWriteArrayList<ActiveLightEffectChangeListener>()
    private val effectListRef = AtomicReference(listOf<ActiveLightEffect>())
    private val writeLock = ReentrantLock()

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
        onUpdate(snapshot)
    }

    override fun removeEffect(lightEffect: ActiveLightEffect) {
        val snapshot: List<ActiveLightEffect> = writeLock.withLock {
            logger.info("Removing light effect $lightEffect")
            val updated = effectListRef.get() - lightEffect
            effectListRef.set(updated)
            updated
        }
        onUpdate(snapshot)
    }

    override fun getEffectWithUuid(uuid: String): Optional<ActiveLightEffect> {
        val effect = effectListRef.get().firstOrNull { it.effectUuid == uuid }
        return if (effect == null) Optional.empty() else Optional.of(effect)
    }

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

    override fun addListener(listener: ActiveLightEffectChangeListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ActiveLightEffectChangeListener) {
        listeners.remove(listener)
    }

    override fun removeAllEffects() {
        writeLock.withLock {
            effectListRef.set(emptyList())
        }
        onUpdate(emptyList())
    }

    private fun onUpdate(updatedEffectList: List<ActiveLightEffect>) {
        listeners.forEach { it.onUpdate(updatedEffectList) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectRegistryImpl::class.java)
    }
}
