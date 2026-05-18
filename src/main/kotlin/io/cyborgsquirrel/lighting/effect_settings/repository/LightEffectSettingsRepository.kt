package io.cyborgsquirrel.lighting.effect_settings.repository

import io.cyborgsquirrel.lighting.effect_settings.entity.LightEffectSettingsEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import java.util.*

interface LightEffectSettingsRepository : CrudRepository<LightEffectSettingsEntity, Long> {
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectSettingsEntity>

    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LightEffectSettingsEntity>

    fun findByTypeAndIsDefault(type: String, isDefault: Boolean): Optional<LightEffectSettingsEntity>

    fun findByIdIn(ids: List<Long>): List<LightEffectSettingsEntity>
}
