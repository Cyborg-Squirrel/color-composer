package io.cyborgsquirrel.jobs.init

import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.LightEffectRegistry
import io.cyborgsquirrel.lighting.effects.service.CreateLightingService
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

/**
 * Server startup job to initialize from the last saved state.
 *
 * This job reads configured effects, filters, palettes, triggers, and clients from the database. Then registers each
 * with the corresponding service (e.g. [TriggerManager], [LightEffectRegistry], [StreamJobManager]).
 */
@Singleton
class LightEffectInitJob(
    private val clientRepository: LedStripClientRepository,
    private val lightEffectRepository: LightEffectRepository,
    private val activeLightEffectService: LightEffectRegistry,
    private val triggerManager: TriggerManager,
    private val createLightingService: CreateLightingService,
    private val streamJobManager: StreamJobManager,
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
                for (effectEntity in effectEntities) {
                    val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
                    val palette = if (effectEntity.palette != null) createLightingService.createPalette(
                        effectEntity.palette!!.settings!!,
                        effectEntity.palette!!.type!!,
                        effectEntity.palette!!.uuid!!,
                        strip.length()
                    ) else null
                    val lightEffect = createLightingService.createEffect(
                        effectEntity.settings,
                        effectEntity.type,
                        palette,
                        strip.length()
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
                        filters = filters,
                    )

                    activeLightEffectService.addOrUpdateEffect(activeEffect)

                    val triggers = createLightingService.effectTriggerFromEntity(effectEntity)
                    if (triggers.isNotEmpty()) {
                        triggers.forEach {
                            triggerManager.addTrigger(it)
                        }
                    }
                }

                val clients = clientRepository.queryAll()
                for (client in clients) {
                    streamJobManager.startStreamingJob(client)
                }

                completed = true
            }
        } catch (ex: Exception) {
            logger.error("Error initializing light effects from database!", ex)
        } finally {
            lock.release()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectInitJob::class.java)
    }
}