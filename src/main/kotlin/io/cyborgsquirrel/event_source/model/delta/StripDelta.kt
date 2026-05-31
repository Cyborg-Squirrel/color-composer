package io.cyborgsquirrel.event_source.model.delta

import com.fasterxml.jackson.annotation.JsonInclude
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.serde.annotation.Serdeable

/**
 * Delta payload for `LedStripUpdated` events. Only the fields that changed are populated; the rest
 * are null and omitted from the serialized JSON.
 */
@Serdeable.Serializable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class StripDelta(
    val name: String? = null,
    val pin: String? = null,
    val length: Int? = null,
    val height: Int? = null,
    val brightness: Int? = null,
    val blendMode: BlendMode? = null,
    val clientUuid: String? = null,
)
