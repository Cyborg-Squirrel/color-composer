package io.cyborgsquirrel.lighting.filters.repository

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.Optional

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectFilterRepository : CrudRepository<LightEffectFilterEntity, Long> {
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectFilterEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): List<LightEffectFilterEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LightEffectFilterEntity>
}