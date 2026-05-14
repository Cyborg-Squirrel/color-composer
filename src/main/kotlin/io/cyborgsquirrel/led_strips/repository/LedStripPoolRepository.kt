package io.cyborgsquirrel.led_strips.repository

import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import java.util.*

interface LedStripPoolRepository : CrudRepository<LedStripPoolEntity, Long> {
    @Join(value = "members", type = Join.Type.LEFT_FETCH)
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LedStripPoolEntity>

    @Join(value = "members", type = Join.Type.LEFT_FETCH)
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    fun queryById(id: Long): Optional<LedStripPoolEntity>

    @Join(value = "members", type = Join.Type.LEFT_FETCH)
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LedStripPoolEntity>
}
