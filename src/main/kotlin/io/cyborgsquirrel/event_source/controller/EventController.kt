package io.cyborgsquirrel.event_source.controller

import io.cyborgsquirrel.event_source.model.SseEvent
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.event_source.api.EventApi
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.sse.Event
import org.reactivestreams.Publisher

@Controller("/events")
class EventController(
    private val sseEventEmitter: SseEventEmitter,
) : EventApi {
    @Get(produces = [MediaType.TEXT_EVENT_STREAM])
    override fun stream(): Publisher<Event<SseEvent>> = sseEventEmitter.events.map { Event.of(it).name(it.type) }
}
