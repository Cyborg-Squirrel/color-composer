package io.cyborgsquirrel

import io.cyborgsquirrel.lighting.job.LightEffectInitJob
import io.cyborgsquirrel.lighting.job.StreamJobManager
import io.cyborgsquirrel.sunrise_sunset.job.SunriseSunsetApiFetchJob
import io.cyborgsquirrel.util.H2WebServer
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.Micronaut.run
import io.micronaut.runtime.server.event.ServerShutdownEvent
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.scheduling.TaskScheduler
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Duration

fun main(args: Array<String>) {
    run(*args)
}

@Singleton
class StartupListener(
    private val h2WebServer: H2WebServer,
    private val sunriseSunsetJob: SunriseSunsetApiFetchJob,
    private val lightEffectInitJob: LightEffectInitJob,
    private val taskScheduler: TaskScheduler,
) :
    ApplicationEventListener<ServerStartupEvent> {

    override fun onApplicationEvent(event: ServerStartupEvent) {
        logger.info("Application started")
        try {
            // Run background task
            taskScheduler.schedule(Duration.ofMillis(0), lightEffectInitJob)
            taskScheduler.schedule("1 0 * * ?", sunriseSunsetJob)
            taskScheduler.schedule(Duration.ofMillis(0), sunriseSunsetJob)
            h2WebServer.start()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartupListener::class.java)
    }
}

@Singleton
class ShutdownListener(
    private val streamJobManager: StreamJobManager,
    private val h2WebServer: H2WebServer,
) :
    ApplicationEventListener<ServerShutdownEvent> {

    override fun onApplicationEvent(event: ServerShutdownEvent) {
        logger.info("Application shutting down")
        try {
            h2WebServer.stop()
            streamJobManager.stopAllJobs()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShutdownListener::class.java)
    }
}