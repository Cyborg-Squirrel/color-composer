package io.cyborgsquirrel.led_strips.repository

import io.cyborgsquirrel.led_strips.entity.PoolMemberLedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface H2PoolMemberLedStripRepository : CrudRepository<PoolMemberLedStripEntity, Long> {
    @Join(value = "pool", type = Join.Type.LEFT_FETCH)
    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<PoolMemberLedStripEntity>

    @Join(value = "pool", type = Join.Type.LEFT_FETCH)
    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    fun findByPool(pool: LedStripPoolEntity): List<PoolMemberLedStripEntity>
}