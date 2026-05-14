package io.cyborgsquirrel.clients.repository

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import java.util.*

interface LedStripClientRepository : CrudRepository<LedStripClientEntity, Long> {
    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LedStripClientEntity>

    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun findByAddress(name: String): Optional<LedStripClientEntity>

    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LedStripClientEntity>
}
