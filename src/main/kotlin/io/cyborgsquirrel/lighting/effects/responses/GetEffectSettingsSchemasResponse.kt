package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsSchema
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetEffectSettingsSchemasResponse(val effectName: String, val schema: EffectSettingsSchema)