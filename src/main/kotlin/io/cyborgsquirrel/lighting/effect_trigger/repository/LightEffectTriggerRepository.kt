package io.cyborgsquirrel.lighting.effect_trigger.repository

import io.cyborgsquirrel.lighting.effect_trigger.entity.LightEffectTriggerEntity
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository

interface LightEffectTriggerRepository : CrudRepository<LightEffectTriggerEntity, Long> {
    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LightEffectTriggerEntity>

    @Join(value = "effect", type = Join.Type.LEFT_FETCH)
    fun findByEffect(effect: LightEffectEntity): List<LightEffectTriggerEntity>
}
