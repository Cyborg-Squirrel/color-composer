package io.cyborgsquirrel.lighting.effect_palette.repository

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteJunctionEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectPaletteJunctionRepository : CrudRepository<LightEffectPaletteJunctionEntity, Long> {
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectPaletteJunctionEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    fun findByPalette(palette: LightEffectPaletteEntity): List<LightEffectPaletteJunctionEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "palette", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): List<LightEffectPaletteJunctionEntity>
}