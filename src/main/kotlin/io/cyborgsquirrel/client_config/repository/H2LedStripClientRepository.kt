package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.LedStripClientEntity
import io.micronaut.context.annotation.Requires
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
@Requires(property = "datasources.default.dialect", value = "H2")
interface H2LedStripClientRepository : CrudRepository<LedStripClientEntity, Long> {
    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun findByName(name: String): Optional<LedStripClientEntity>

    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun findByAddress(name: String): Optional<LedStripClientEntity>

    @Join(value = "strips", type = Join.Type.LEFT_FETCH)
    fun findAllByIdGreaterThan(id: Int = 0): List<LedStripClientEntity>
}