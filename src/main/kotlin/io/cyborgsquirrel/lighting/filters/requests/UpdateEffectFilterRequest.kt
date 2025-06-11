package io.cyborgsquirrel.lighting.filters.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectFilterRequest(val name: String?, val effectUuids: List<String>, val settings: Map<String, Any>?)