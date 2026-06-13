package io.cyborgsquirrel.clients.repository

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import java.util.*

interface LedStripClientRepository : CrudRepository<LedStripClientEntity, Long> {
    // Command-side finders: eagerly fetch `strips` for invariant checks (restart-on-update,
    // delete's empties guard).
    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LedStripClientEntity>

    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun findByAddress(name: String): Optional<LedStripClientEntity>

    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LedStripClientEntity>

    // Query-side finder (CQRS read path): join-free, since the read path never touches `strips`.
    // The list read uses the inherited join-free findAll().
    fun getByUuid(uuid: String): Optional<LedStripClientEntity>
}
