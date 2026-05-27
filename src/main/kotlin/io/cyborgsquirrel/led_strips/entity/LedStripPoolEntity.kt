package io.cyborgsquirrel.led_strips.entity

import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@MappedEntity("led_strip_pools")
data class LedStripPoolEntity(
    @param:Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "pool")
    var members: Set<PoolMemberLedStripEntity> = setOf(),

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "pool")
    var effects: Set<LightEffectEntity> = setOf(),

    var uuid: String,

    var name: String,

    @MappedEntity("pool_type")
    @Enumerated(EnumType.STRING)
    var poolType: PoolType,

    @MappedEntity("blend_mode")
    @Enumerated(EnumType.STRING)
    var blendMode: BlendMode,
)