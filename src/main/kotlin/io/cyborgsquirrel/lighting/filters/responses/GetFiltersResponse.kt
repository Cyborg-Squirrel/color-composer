package io.cyborgsquirrel.lighting.filters.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetFiltersResponse(val filters: List<GetFilterResponse>)