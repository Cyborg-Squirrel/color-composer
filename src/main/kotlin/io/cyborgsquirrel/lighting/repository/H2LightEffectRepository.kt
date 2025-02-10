package io.cyborgsquirrel.lighting.repository

import io.cyborgsquirrel.entity.LightEffectEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectRepository : CrudRepository<LightEffectEntity, Long> {
     @Join(value = "associations", type = Join.Type.LEFT_FETCH)
     fun findByName(name: String): Optional<LightEffectEntity>
}