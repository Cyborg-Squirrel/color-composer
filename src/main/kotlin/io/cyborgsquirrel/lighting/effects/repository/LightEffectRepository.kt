package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import java.util.*

@Join(value = "strip", type = Join.Type.LEFT_FETCH)
@Join(value = "pool", type = Join.Type.LEFT_FETCH)
@Join(value = "palette", type = Join.Type.LEFT_FETCH)
@Join(value = "pool.members", type = Join.Type.LEFT_FETCH)
@Join(value = "triggers", type = Join.Type.LEFT_FETCH)
@Join(value = "filterJunctions", type = Join.Type.LEFT_FETCH)
interface LightEffectRepository : CrudRepository<LightEffectEntity, Long> {
    fun queryAll(): List<LightEffectEntity>
    fun findByStrip(strip: LedStripEntity): List<LightEffectEntity>
    fun findByPool(pool: LedStripPoolEntity): List<LightEffectEntity>
    fun findByStatusIn(statuses: List<LightEffectStatus>): List<LightEffectEntity>
    fun findByUuid(uuid: String): Optional<LightEffectEntity>
    fun findByUuidIn(uuid: List<String>): List<LightEffectEntity>
    fun findByIdIn(ids: List<Long>): List<LightEffectEntity>
}
