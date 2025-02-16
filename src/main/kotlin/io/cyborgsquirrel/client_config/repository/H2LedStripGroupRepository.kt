package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.LedStripGroupEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2LedStripGroupRepository : CrudRepository<LedStripGroupEntity, Long> {
     @Join(value = "strips", type = Join.Type.LEFT_FETCH)
     @Join(value = "associations", type = Join.Type.LEFT_FETCH)
     fun findByName(name: String): Optional<LedStripGroupEntity>

     @Join(value = "strips", type = Join.Type.LEFT_FETCH)
     @Join(value = "associations", type = Join.Type.LEFT_FETCH)
     fun findByUuid(uuid: String): Optional<LedStripGroupEntity>
}