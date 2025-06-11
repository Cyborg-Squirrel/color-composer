package io.cyborgsquirrel.lighting.filters.repository

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterJunctionEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectFilterJunctionRepository : CrudRepository<LightEffectFilterJunctionEntity, Long> {
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "filter", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectFilterJunctionEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "filter", type = Join.Type.LEFT_FETCH)
    fun findByFilter(filter: LightEffectFilterEntity): List<LightEffectFilterJunctionEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "filter", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): List<LightEffectFilterJunctionEntity>
}