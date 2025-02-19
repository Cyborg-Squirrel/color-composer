package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LedStripGroupEntity
import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectRepository : CrudRepository<LightEffectEntity, Long> {
    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    fun findByStrip(strip: LedStripEntity): List<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    fun findByGroup(group: LedStripGroupEntity): List<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    fun findByStatus(status: LightEffectStatus): List<LightEffectEntity>
}