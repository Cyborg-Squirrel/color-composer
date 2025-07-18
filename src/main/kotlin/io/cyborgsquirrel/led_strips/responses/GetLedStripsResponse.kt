package io.cyborgsquirrel.led_strips.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetLedStripsResponse(val strips: List<GetLedStripResponse>)