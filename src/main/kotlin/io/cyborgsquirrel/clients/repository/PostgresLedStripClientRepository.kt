package io.cyborgsquirrel.clients.repository

import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@JdbcRepository(dialect = Dialect.POSTGRES)
@Requires(property = "datasources.default.dialect", value = "POSTGRES")
@Replaces(value = H2LedStripClientRepository::class)
interface PostgresLedStripClientRepository : H2LedStripClientRepository