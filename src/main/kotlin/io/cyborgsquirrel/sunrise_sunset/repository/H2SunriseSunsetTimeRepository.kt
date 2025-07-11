package io.cyborgsquirrel.sunrise_sunset.repository

import io.cyborgsquirrel.sunrise_sunset.entity.LocationConfigEntity
import io.cyborgsquirrel.sunrise_sunset.entity.SunriseSunsetTimeEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2SunriseSunsetTimeRepository : CrudRepository<SunriseSunsetTimeEntity, Long> {
    @Join(value = "location", type = Join.Type.RIGHT_FETCH)
    fun findAllOrderByYmd(pageable: Pageable): List<SunriseSunsetTimeEntity>

    @Join(value = "location", type = Join.Type.RIGHT_FETCH)
    fun findByYmdEquals(ymd: String): List<SunriseSunsetTimeEntity>

    @Join(value = "location", type = Join.Type.RIGHT_FETCH)
    fun findByYmdEqualsAndLocation(ymd: String, location: LocationConfigEntity): Optional<SunriseSunsetTimeEntity>

    @Join(value = "location", type = Join.Type.RIGHT_FETCH)
    fun findByLocationOrderByYmd(location: LocationConfigEntity): List<SunriseSunsetTimeEntity>
}