package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LedStripGroupEntity
import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.entity.LightEffectLedStripAssociationEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectLedStripAssociationRepository : CrudRepository<LightEffectLedStripAssociationEntity, Long> {

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    fun findByStrip(strip: LedStripEntity): Optional<LightEffectLedStripAssociationEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    fun findByGroup(group: LedStripGroupEntity): Optional<LightEffectLedStripAssociationEntity>

    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): Optional<LightEffectLedStripAssociationEntity>
}