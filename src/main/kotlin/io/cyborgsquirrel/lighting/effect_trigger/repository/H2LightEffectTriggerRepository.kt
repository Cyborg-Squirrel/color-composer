package io.cyborgsquirrel.lighting.effect_trigger.repository

import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.entity.LightEffectTriggerEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectTriggerRepository : CrudRepository<LightEffectTriggerEntity, Long> {
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectTriggerEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): List<LightEffectTriggerEntity>
}