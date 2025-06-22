package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.CreateLightingService
import io.cyborgsquirrel.lighting.limits.PowerLimiterService
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

@Singleton
class LightEffectInitJob(
    private val lightEffectRepository: H2LightEffectRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val triggerManager: TriggerManager,
    private val createLightingService: CreateLightingService,
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
                    val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
                    val palette = if (effectEntity.palette != null) createLightingService.createPalette(
                        effectEntity.palette!!.settings!!,
                        effectEntity.palette!!.type!!,
                        effectEntity.palette!!.uuid!!,
                        strip
                    ) else null
                    val lightEffect = createLightingService.createEffect(
                        effectEntity.settings!!,
                        effectEntity.type!!,
                        palette,
                        strip
                    )
                    val filters = createLightingService.createEffectFilterFromEntity(effectEntity)
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

                    val triggers = createLightingService.effectTriggerFromEntity(effectEntity)
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