package io.cyborgsquirrel.lighting.filters.repository

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterJunctionEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository

interface LightEffectFilterJunctionRepository : CrudRepository<LightEffectFilterJunctionEntity, Long> {
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "filter", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectFilterJunctionEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "filter", type = Join.Type.LEFT_FETCH)
    fun findByFilter(filter: LightEffectFilterEntity): List<LightEffectFilterJunctionEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    @Join(value = "filter", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): List<LightEffectFilterJunctionEntity>
}
