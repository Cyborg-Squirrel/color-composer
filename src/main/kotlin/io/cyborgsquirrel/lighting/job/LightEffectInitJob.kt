package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.lighting.effect_trigger.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.CreateLightingHelper
import io.cyborgsquirrel.lighting.limits.PowerLimiterService
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

@Singleton
class LightEffectInitJob(
    private val lightEffectRepository: H2LightEffectRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val triggerManager: TriggerManager,
    private val createLightingHelper: CreateLightingHelper,
    private val limiterService: PowerLimiterService,
) : Runnable {

    // Flag tracking if this init job has run.
    // Should only run once at startup.
    private var completed = false
    private val lock = Semaphore(1)

    override fun run() {
        try {
            lock.acquire()
            if (!completed) {
                val effectEntities = lightEffectRepository.queryAll()
                val strips = effectEntities.mapNotNull { it.strip }.distinctBy { it.uuid }
                strips.forEach {
                    if (it.uuid != null && it.powerLimit != null) limiterService.setLimit(it.uuid!!, it.powerLimit!!)
                }
                for (effectEntity in effectEntities) {
                    val strip = createLightingHelper.ledStripFromEffectEntity(effectEntity)
                    val lightEffect = createLightingHelper.createEffect(
                        effectEntity.settings!!,
                        effectEntity.type!!,
                        strip.getLength()
                    )
                    val filters = createLightingHelper.createEffectFilterFromEntity(effectEntity)
                    val activeEffect = ActiveLightEffect(
                        effectUuid = effectEntity.uuid!!,
                        // TODO add priority to persistence layer
                        priority = 0,
                        skipFramesIfBlank = true,
                        status = effectEntity.status!!,
                        strip = strip,
                        effect = lightEffect,
                        filters = filters
                    )

                    activeLightEffectRegistry.addOrUpdateEffect(activeEffect)

                    val triggers = createLightingHelper.effectTriggerFromEntity(effectEntity)
                    if (triggers.isNotEmpty()) {
                        triggers.forEach {
                            triggerManager.addTrigger(it)
                        }
                    }
                }

                completed = true
            }
        } catch (ex: java.lang.Exception) {
            logger.error("Error initializing light effects from database!", ex)
        } finally {
            lock.release()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectInitJob::class.java)
    }
}