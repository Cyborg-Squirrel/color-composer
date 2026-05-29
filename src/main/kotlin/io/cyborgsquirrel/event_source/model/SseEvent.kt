package io.cyborgsquirrel.event_source.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.serde.annotation.Serdeable

/**
 * Base type for all server-sent events. Events are serialize-only (server -> client).
 *
 * In addition to the affected resource's [uuid] and the event [type] discriminator, each event
 * carries a [data] payload describing what changed:
 *  - Created events carry the full resource object, matching its `GET` API shape.
 *  - Updated events carry a `<Type>Delta` object holding only the fields that changed.
 *  - Deleted events carry no payload ([data] is null and omitted from the JSON).
 */
@Serdeable.Serializable
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class SseEvent(val uuid: String, val data: Any?) {
    val type: String get() = this::class.simpleName ?: ""
}
