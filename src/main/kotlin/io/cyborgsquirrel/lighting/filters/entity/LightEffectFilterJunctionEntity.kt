package io.cyborgsquirrel.lighting.filters.entity

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation

@MappedEntity("light_effect_filter_junctions")
data class LightEffectFilterJunctionEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var effect: LightEffectEntity? = null,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var filter: LightEffectFilterEntity? = null,
) {
    // Overrides to prevent infinite looping

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LightEffectFilterJunctionEntity) return false

        if (id != other.id) return false
        if (effect?.id != other.effect?.id) return false
        if (filter?.id != other.filter?.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (effect?.id ?: 0).hashCode()
        result = 31 * result + (filter?.id ?: 0).hashCode()
        return result
    }

    override fun toString(): String {
        return "LightEffectFilterJunctionEntity(id=$id, effect=${effect?.id}, filter=${filter?.id})"
    }
}