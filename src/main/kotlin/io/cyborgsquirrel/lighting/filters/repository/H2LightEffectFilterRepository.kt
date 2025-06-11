package io.cyborgsquirrel.lighting.filters.repository

import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*


@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectFilterRepository : CrudRepository<LightEffectFilterEntity, Long> {
    @Join(value = "effectJunctions", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectFilterEntity>

    @Join(value = "effectJunctions", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LightEffectFilterEntity>

    @Join(value = "effectJunctions", type = Join.Type.LEFT_FETCH)
    fun findByIdIn(ids: List<Long>): List<LightEffectFilterEntity>
}