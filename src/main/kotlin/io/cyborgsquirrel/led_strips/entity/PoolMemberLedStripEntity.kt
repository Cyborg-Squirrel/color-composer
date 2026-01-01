package io.cyborgsquirrel.led_strips.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation

@MappedEntity("pool_member_led_strips")
data class PoolMemberLedStripEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var pool: LedStripPoolEntity?,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var strip: LedStripEntity?,

    var inverted: Boolean?,

    var poolIndex: Int?
) {
    // Overrides to prevent infinite looping

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoolMemberLedStripEntity) return false

        if (id != other.id) return false
        if (strip?.id != other.strip?.id) return false
        if (pool?.id != other.pool?.id) return false
        if (inverted != other.inverted) return false
        if (poolIndex != other.poolIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = poolIndex.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (strip?.id ?: 0).hashCode()
        result = 31 * result + (pool?.id ?: 0).hashCode()
        result = 31 * result + (inverted ?: 0).hashCode()
        return result
    }

    override fun toString(): String {
        return "PoolMemberLedStripEntity(strip=${strip?.id}, pool=${pool?.id}, id=$id, inverted=$inverted, poolIndex=$poolIndex)"
    }
}