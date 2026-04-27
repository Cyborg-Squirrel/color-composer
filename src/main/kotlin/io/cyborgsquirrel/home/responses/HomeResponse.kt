package io.cyborgsquirrel.home.responses

import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.led_strips.responses.GetLedStripResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class HomeResponse(
    val totalClients: Int,
    val totalStrips: Int,
    val totalEffects: Int,
    val totalPalettes: Int,
    val activeEffects: List<GetEffectResponse>,
    val strips: List<GetLedStripResponse>,
    val clients: List<GetClientResponse>
)
