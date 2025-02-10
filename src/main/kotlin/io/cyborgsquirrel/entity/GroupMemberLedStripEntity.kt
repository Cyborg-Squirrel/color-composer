package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("group_member_led_strips")
data class GroupMemberLedStripEntity(
    @Id
    @GeneratedValue
    var id: Int = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var group: LedStripGroupEntity?,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var strip: LedStripEntity?,

    var inverted: Boolean?,

    var groupIndex: Int?,
)