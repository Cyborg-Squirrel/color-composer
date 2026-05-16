package io.cyborgsquirrel.event_source.api

import io.cyborgsquirrel.event_source.model.SseEvent
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.sse.Event
import org.reactivestreams.Publisher

interface EventApi {

    @Get(produces = [MediaType.TEXT_EVENT_STREAM])
    fun stream(): Publisher<Event<SseEvent>>
}