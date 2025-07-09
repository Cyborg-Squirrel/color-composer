package io.cyborgsquirrel.lighting.job.streaming

import io.cyborgsquirrel.clients.config.ConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.job.streaming.nightdriver.NightDriverSocketJob
import io.cyborgsquirrel.lighting.job.streaming.pi_client.WebSocketJob
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.websocket.WebSocketClient
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

@Singleton
class StreamJobManagerImpl(
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val clientRepository: H2LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private val configClient: ConfigClient,
) : StreamJobManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobMap = mutableMapOf<String, Pair<ClientStreamingJob, Job>>()
    private val lock = Semaphore(1)

    override fun startWebsocketJob(client: LedStripClientEntity) {
        logger.info("Starting websocket job for $client")
        try {
            lock.acquire()
            val wsJob = when (client.clientType) {
                ClientType.Pi -> {
                    WebSocketJob(
                        webSocketClient,
                        renderer,
                        triggerManager,
                        clientRepository,
                        timeHelper,
                        configClient,
                        client
                    )
                }

                ClientType.NightDriver -> {
                    NightDriverSocketJob(
                        renderer,
                        triggerManager,
                        clientRepository,
                        timeHelper,
                        client
                    )
                }

                null -> throw Exception("No clientType specified!")
            }

            if (jobMap.containsKey((client.uuid!!))) {
                stopWebsocketJob(client)
            }

            val job = wsJob.start(scope)
            jobMap[client.uuid!!] = Pair(wsJob, job)
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
            jobMap[client.uuid!!]?.first?.onDataUpdate()
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
            jobMap[client.uuid!!]?.first?.dispose()
            runBlocking { jobMap[client.uuid!!]?.second?.cancel() }
            jobMap.remove(client.uuid!!)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    override fun stopAllJobs() {
        logger.info("Stopping all websocket jobs...")
        try {
            lock.acquire()
            for (clientUuid in jobMap.keys) {
                jobMap[clientUuid]?.first?.dispose()
                runBlocking { jobMap[clientUuid]?.second?.cancel() }
            }

            jobMap.clear()
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamJobManagerImpl::class.java)
    }
}