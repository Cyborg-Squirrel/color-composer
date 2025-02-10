package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("light_effect_led_strip_associations")
data class LightEffectLedStripAssociationEntity(
    @Id
    @GeneratedValue
    var id: Int = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var strip: LedStripEntity? = null,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var group: LedStripGroupEntity? = null,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var effect: LightEffectEntity?,
) {
    // Overrides to prevent infinite looping

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LightEffectLedStripAssociationEntity) return false

        if (id != other.id) return false
        if (strip?.id != other.strip?.id) return false
        if (group?.id != other.group?.id) return false
        if (effect?.id != other.effect?.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 31 + id
        result = 31 * result + (strip?.id ?: 0).hashCode()
        result = 31 * result + (group?.id ?: 0).hashCode()
        result = 31 * result + (effect?.id ?: 0).hashCode()
        return result.toInt()
    }

    override fun toString(): String {
        return "LightEffectLedStripAssociationEntity(strip=${strip?.id}, group=${group?.id}, effect=${effect?.id}, id=$id)"
    }
}