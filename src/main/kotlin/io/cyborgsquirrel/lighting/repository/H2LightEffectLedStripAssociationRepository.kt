package io.cyborgsquirrel.lighting.repository

import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.entity.LedStripGroupEntity
import io.cyborgsquirrel.entity.LightEffectLedStripAssociationEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectLedStripAssociationRepository : CrudRepository<LightEffectLedStripAssociationEntity, Long> {
    fun findByStrip(strip: LedStripEntity): Optional<LightEffectLedStripAssociationEntity>

    fun findByGroup(group: LedStripGroupEntity): Optional<LightEffectLedStripAssociationEntity>
}