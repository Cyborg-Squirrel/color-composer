package io.cyborgsquirrel.lighting.effect_trigger.repository

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectTriggerRepository : LightEffectTriggerRepository
