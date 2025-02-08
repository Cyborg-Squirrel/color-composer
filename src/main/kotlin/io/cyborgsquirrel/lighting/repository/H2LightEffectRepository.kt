package io.cyborgsquirrel.lighting.repository

import io.cyborgsquirrel.entity.LightEffectEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectRepository : CrudRepository<LightEffectEntity, Long> {
     fun findByName(name: String): Optional<LightEffectEntity>
}