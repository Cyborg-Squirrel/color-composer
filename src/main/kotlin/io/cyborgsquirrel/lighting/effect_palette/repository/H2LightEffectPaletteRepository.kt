package io.cyborgsquirrel.lighting.effect_palette.repository

import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*


@JdbcRepository(dialect = Dialect.H2)
interface H2LightEffectPaletteRepository : CrudRepository<LightEffectPaletteEntity, Long> {
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectPaletteEntity>

    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LightEffectPaletteEntity>
}