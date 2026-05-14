package io.cyborgsquirrel.sunrise_sunset.repository

import io.micronaut.context.annotation.Requires
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@JdbcRepository(dialect = Dialect.H2)
@Requires(property = "datasources.default.dialect", value = "H2")
interface H2LocationConfigRepository : LocationConfigRepository
