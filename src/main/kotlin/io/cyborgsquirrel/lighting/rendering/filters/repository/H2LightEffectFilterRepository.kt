package io.cyborgsquirrel.lighting.rendering.filters.repository

import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.entity.LightEffectFilterEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectFilterRepository : CrudRepository<LightEffectFilterEntity, Long> {
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectFilterEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): List<LightEffectFilterEntity>
}