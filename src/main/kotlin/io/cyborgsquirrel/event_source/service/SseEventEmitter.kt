package io.cyborgsquirrel.event_source.service

import io.cyborgsquirrel.event_source.model.SseEvent
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Singleton
class SseEventEmitter {
    private val sink: Sinks.Many<SseEvent> = Sinks.many().multicast().onBackpressureBuffer()

    val events: Flux<SseEvent> = sink.asFlux()

    fun emit(event: SseEvent) {
        val result = sink.tryEmitNext(event)
        if (result.isFailure) {
            logger.warn("Failed to emit SSE event {}: {}", event, result)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SseEventEmitter::class.java)
    }
}
