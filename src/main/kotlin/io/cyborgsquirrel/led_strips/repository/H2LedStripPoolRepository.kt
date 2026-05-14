package io.cyborgsquirrel.led_strips.repository

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@JdbcRepository(dialect = Dialect.H2)
interface H2LedStripPoolRepository : LedStripPoolRepository
