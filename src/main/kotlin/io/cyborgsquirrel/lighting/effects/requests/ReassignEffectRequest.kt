package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ReassignEffectRequest(val unassign: Boolean, val targetStripUuid: String?, val targetPoolUuid: String?)