package io.cyborgsquirrel.event_source.model.delta

import com.fasterxml.jackson.annotation.JsonInclude
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.serde.annotation.Serdeable

/**
 * Delta payload for `LightEffectUpdated` events. Only the fields that changed are populated; the rest
 * are null and omitted from the serialized JSON.
 *
 * Note: because null fields are omitted, clearing a value (e.g. unassigning a palette, or
 * unassigning an effect from its strip/pool) cannot be represented here and is not reported in the
 * delta. Consumers should treat an omitted field as "unchanged".
 */
@Serdeable.Serializable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EffectDelta(
    val name: String? = null,
    val paletteUuid: String? = null,
    val settingsUuid: String? = null,
    val status: LightEffectStatus? = null,
    val stripUuid: String? = null,
    val poolUuid: String? = null,
    val layer: Int? = null,
)
