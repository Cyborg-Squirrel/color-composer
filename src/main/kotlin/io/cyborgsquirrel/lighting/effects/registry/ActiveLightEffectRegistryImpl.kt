package io.cyborgsquirrel.lighting.effects.registry

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Semaphore

@Singleton
class ActiveLightEffectRegistryImpl : ActiveLightEffectRegistry {
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
        } finally {
            lock.release()
        }
    }

    override fun removeEffect(lightEffect: ActiveLightEffect) {
        try {
            lock.acquire()
            logger.info("Removing light effect $lightEffect")
            effectList.remove(lightEffect)
        } finally {
            lock.release()
        }
    }

    override fun findEffectsWithStatus(status: LightEffectStatus): List<ActiveLightEffect> {
        val matchingEffects = mutableListOf<ActiveLightEffect>()
        try {
            lock.acquire()
            matchingEffects.addAll(effectList.filter { it.status == status })
        } finally {
            lock.release()
        }

        return matchingEffects
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

    override fun getAllEffectsForStrip(stripUuid: String): List<ActiveLightEffect> {
        val effects = mutableListOf<ActiveLightEffect>()
        try {
            lock.acquire()
            effects.addAll(effectList.filter {
                it.strip.getUuid() == stripUuid
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

    override fun reset() {
        try {
            lock.acquire()
            effectList.clear()
        } finally {
            lock.release()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ActiveLightEffectRegistryImpl::class.java)
    }
}