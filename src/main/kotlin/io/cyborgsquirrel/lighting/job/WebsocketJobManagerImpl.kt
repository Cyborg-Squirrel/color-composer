package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.clients.config.ConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.websocket.WebSocketClient
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

@Singleton
class WebsocketJobManagerImpl(
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val clientRepository: H2LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private val configClient: ConfigClient,
): WebsocketJobManager {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private val jobMap = mutableMapOf<String, WebSocketJob>()
    private val lock = Semaphore(1)

    override fun startWebsocketJob(client: LedStripClientEntity) {
        logger.info("Starting websocket job for $client")
        try {
            lock.acquire()
            val job = WebSocketJob(
                webSocketClient,
                renderer,
                triggerManager,
                clientRepository,
                timeHelper,
                configClient,
                client
            )

            if (jobMap.containsKey((client.uuid!!))) {
                stopWebsocketJob(client)
            }

            jobMap[client.uuid!!] = job
            executor.submit(job)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    override fun updateJob(client: LedStripClientEntity) {
        logger.info("Updating websocket job for $client")
        try {
            lock.acquire()
            jobMap[client.uuid!!]?.onDataUpdate()
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    override fun stopWebsocketJob(client: LedStripClientEntity) {
        logger.info("Stopping websocket job for $client")
        try {
            lock.acquire()
            jobMap[client.uuid!!]?.dispose()
            jobMap.remove(client.uuid!!)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    override fun stopAllJobs() {
        logger.info("Starting all websocket jobs...")
        try {
            lock.acquire()
            for (clientUuid in jobMap.keys) {
                jobMap[clientUuid]?.dispose()
            }

            jobMap.clear()
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebsocketJobManagerImpl::class.java)
    }
}