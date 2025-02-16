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
) {
    // Overrides to prevent infinite looping

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberLedStripEntity) return false

        if (id != other.id) return false
        if (strip?.id != other.strip?.id) return false
        if (group?.id != other.group?.id) return false
        if (inverted != other.inverted) return false
        if (groupIndex != other.groupIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupIndex.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (strip?.id ?: 0).hashCode()
        result = 31 * result + (group?.id ?: 0).hashCode()
        result = 31 * result + (inverted ?: 0).hashCode()
        return result
    }

    override fun toString(): String {
        return "GroupMemberLedStripEntity(strip=${strip?.id}, group=${group?.id}, id=$id, inverted=$inverted, groupIndex=$groupIndex)"
    }
}