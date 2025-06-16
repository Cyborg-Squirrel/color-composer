package io.cyborgsquirrel.lighting.effects.entity

import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripGroupEntity
import io.cyborgsquirrel.lighting.effect_trigger.entity.LightEffectTriggerEntity
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterJunctionEntity
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteJunctionEntity
import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@MappedEntity("light_effects")
data class LightEffectEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var strip: LedStripEntity? = null,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var group: LedStripGroupEntity? = null,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "effect")
    var triggers: Set<LightEffectTriggerEntity> = setOf(),

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "effect")
    var filterJunctions: Set<LightEffectFilterJunctionEntity> = setOf(),

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "effect")
    var paletteJunctions: Set<LightEffectPaletteJunctionEntity> = setOf(),

    var uuid: String? = null,

    @TypeDef(type = DataType.JSON)
    var settings: Map<String, Any>?,

    var type: String?,

    var name: String?,

    @Enumerated(EnumType.STRING)
    var status: LightEffectStatus?,
) {
    // Overrides to prevent infinite looping

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LightEffectEntity) return false

        if (id != other.id) return false
        if (strip?.id != other.strip?.id) return false
        if (group?.id != other.group?.id) return false
        if (uuid != other.uuid) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (settings != other.settings) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (strip?.id ?: 0).hashCode()
        result = 31 * result + (group?.id ?: 0).hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + settings.hashCode()
        return result
    }

    override fun toString(): String {
        return "LightEffectLedStripAssociationEntity(strip=${strip?.id}, group=${group?.id}, effectType=$type, name=$name, id=$id, uuid=$uuid, status=$status, settings=$settings)"
    }
}