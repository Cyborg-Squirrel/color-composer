package io.cyborgsquirrel.sunrise_sunset.repository

import io.cyborgsquirrel.entity.LocationConfigEntity
import io.micronaut.context.annotation.Requires
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
@Requires(property = "datasources.default.dialect", value = "H2")
interface H2LocationConfigRepository : CrudRepository<LocationConfigEntity, Long> {
    @Join(value = "sunriseSunsetTimes", type = Join.Type.LEFT_FETCH)
    fun findByIdGreaterThan(id: Int = 0): List<LocationConfigEntity>

    @Join(value = "sunriseSunsetTimes", type = Join.Type.LEFT_FETCH)
    fun findByActiveTrue(): Optional<LocationConfigEntity>
}