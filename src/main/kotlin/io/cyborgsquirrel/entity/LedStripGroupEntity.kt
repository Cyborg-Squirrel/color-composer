package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("led_strip_groups")
data class LedStripGroupEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "group")
    var members: Set<GroupMemberLedStripEntity> = setOf(),

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "group")
    var effects: Set<LightEffectEntity> = setOf(),

    var uuid: String? = null,

    var name: String? = null,
)