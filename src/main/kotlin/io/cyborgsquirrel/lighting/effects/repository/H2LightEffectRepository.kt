package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripGroupEntity
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectRepository : CrudRepository<LightEffectEntity, Long> {
    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    @Join(value = "triggers", type = Join.Type.LEFT_FETCH)
    @Join(value = "filterJunctions", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    @Join(value = "triggers", type = Join.Type.LEFT_FETCH)
    @Join(value = "filterJunctions", type = Join.Type.LEFT_FETCH)
    fun findByStrip(strip: LedStripEntity): List<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    @Join(value = "triggers", type = Join.Type.LEFT_FETCH)
    @Join(value = "filterJunctions", type = Join.Type.LEFT_FETCH)
    fun findByGroup(group: LedStripGroupEntity): List<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    @Join(value = "triggers", type = Join.Type.LEFT_FETCH)
    @Join(value = "filterJunctions", type = Join.Type.LEFT_FETCH)
    fun findByStatus(status: LightEffectStatus): List<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    @Join(value = "triggers", type = Join.Type.LEFT_FETCH)
    @Join(value = "filterJunctions", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LightEffectEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    @Join(value = "group.members", type = Join.Type.LEFT_FETCH)
    @Join(value = "triggers", type = Join.Type.LEFT_FETCH)
    @Join(value = "filterJunctions", type = Join.Type.LEFT_FETCH)
    fun findByIdIn(ids: List<Long>): List<LightEffectEntity>
}