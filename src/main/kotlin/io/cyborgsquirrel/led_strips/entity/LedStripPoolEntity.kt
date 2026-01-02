package io.cyborgsquirrel.led_strips.entity

import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@MappedEntity("led_strip_pools")
data class LedStripPoolEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "pool")
    var members: Set<PoolMemberLedStripEntity> = setOf(),

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "pool")
    var effects: Set<LightEffectEntity> = setOf(),

    var uuid: String? = null,

    var name: String? = null,

    @MappedEntity("pool_type")
    @Enumerated(EnumType.STRING)
    var poolType: PoolType?,
)