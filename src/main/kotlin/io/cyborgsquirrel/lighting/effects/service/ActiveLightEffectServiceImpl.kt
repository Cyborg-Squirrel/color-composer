package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Semaphore

typealias ActiveEffectListUpdateCallback = (List<ActiveLightEffect>) -> Unit

/**
 * A service for memory storage of [ActiveLightEffect]s.
 */
@Singleton
class ActiveLightEffectServiceImpl : ActiveLightEffectService {
    private val listeners = mutableListOf<ActiveEffectListUpdateCallback>()
    private var effectList = mutableListOf<ActiveLightEffect>()
    private val lock = Semaphore(1)

    override fun addOrUpdateEffect(lightEffect: ActiveLightEffect) {
        try {
            lock.acquire()
            if (effectList.none { it.effectUuid == lightEffect.effectUuid }) {
                logger.info("New light effect $lightEffect")
                effectList.add(lightEffect)
            } else {
                logger.info("Updating light effect $lightEffect")
                effectList.replaceAll { if (it.effectUuid == lightEffect.effectUuid) lightEffect else it }
            }

            onUpdate(effectList)
        } finally {
            lock.release()
        }
    }

    override fun removeEffect(lightEffect: ActiveLightEffect) {
        try {
            lock.acquire()
            logger.info("Removing light effect $lightEffect")
            effectList.remove(lightEffect)
            onUpdate(effectList)
        } finally {
            lock.release()
        }
    }

    override fun getEffectWithUuid(uuid: String): Optional<ActiveLightEffect> {
        val effect: ActiveLightEffect?
        try {
            lock.acquire()
            effect = effectList.firstOrNull { it.effectUuid == uuid }
        } finally {
            lock.release()
        }

        return if (effect == null) Optional.empty() else Optional.of(effect)
    }

    override fun getEffectsForClient(clientUuid: String): List<ActiveLightEffect> {
        val effects = mutableListOf<ActiveLightEffect>()
        try {
            lock.acquire()
            effects.addAll(effectList.filter {
                when (it.strip) {
                    is LedStripPoolModel -> it.strip.clientUuids().contains(clientUuid)
                    is SingleLedStripModel -> it.strip.clientUuid == clientUuid
                }
            })
        } finally {
            lock.release()
        }

        return effects
    }

    override fun getAllEffectsForStrip(stripUuid: String): List<ActiveLightEffect> {
        val effects = mutableListOf<ActiveLightEffect>()
        try {
            lock.acquire()
            effects.addAll(effectList.filter {
                it.strip.uuid == stripUuid
            })
        } finally {
            lock.release()
        }

        return effects
    }

    override fun getAllEffects(): List<ActiveLightEffect> {
        val effects = mutableListOf<ActiveLightEffect>()
        try {
            lock.acquire()
            effects.addAll(effectList)
        } finally {
            lock.release()
        }

        return effects
    }

    override fun addListener(listener: ActiveEffectListUpdateCallback) {
        listeners.add(listener)
    }

    override fun removeLister(listener: ActiveEffectListUpdateCallback) {
        listeners.remove(listener)
    }

    override fun removeAllEffects() {
        try {
            lock.acquire()
            effectList.clear()
            onUpdate(effectList)
        } finally {
            lock.release()
        }
    }

    private fun onUpdate(updatedEffectList: List<ActiveLightEffect>) {
        listeners.forEach { it(updatedEffectList) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ActiveLightEffectServiceImpl::class.java)
    }
}