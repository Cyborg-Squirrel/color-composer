package io.cyborgsquirrel.event_source.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
sealed class SseEvent(val uuid: String) {
    val type: String get() = this::class.simpleName ?: ""
}