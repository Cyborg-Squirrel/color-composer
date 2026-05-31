package io.cyborgsquirrel.event_source.model.delta

import com.fasterxml.jackson.annotation.JsonInclude
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.micronaut.serde.annotation.Serdeable

/**
 * Delta payload for `LedClientUpdated` events. Only the fields that changed are populated; the rest
 * are null and omitted from the serialized JSON.
 */
@Serdeable.Serializable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ClientDelta(
    val name: String? = null,
    val address: String? = null,
    val colorOrder: ColorOrder? = null,
    val apiPort: Int? = null,
    val wsPort: Int? = null,
    val powerLimit: Int? = null,
    val fps: Int? = null,
    val fadeTimeoutMillis: Int? = null,
)
