package io.cyborgsquirrel.event_source.model.delta

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.serde.annotation.Serdeable

/**
 * Delta payload for `EffectSettingsUpdated` events. Only the fields that changed are populated; the
 * rest are null and omitted from the serialized JSON.
 */
@Serdeable.Serializable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EffectSettingsDelta(
    val name: String? = null,
    val settings: Map<String, Any>? = null,
    val isDefault: Boolean? = null,
    val skipFramesIfBlank: Boolean? = null,
)
