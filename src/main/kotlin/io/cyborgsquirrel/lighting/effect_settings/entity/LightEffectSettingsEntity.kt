package io.cyborgsquirrel.lighting.effect_settings.entity

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType

@MappedEntity("light_effect_settings")
data class LightEffectSettingsEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "effectSettings")
    var effects: Set<LightEffectEntity> = setOf(),

    var uuid: String,
    var type: String,
    var name: String,
    var isDefault: Boolean = false,

    @TypeDef(type = DataType.JSON)
    var settings: Map<String, Any>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LightEffectSettingsEntity) return false
        if (id != other.id) return false
        if (uuid != other.uuid) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (isDefault != other.isDefault) return false
        if (settings != other.settings) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + isDefault.hashCode()
        result = 31 * result + settings.hashCode()
        return result
    }

    override fun toString(): String =
        "LightEffectSettingsEntity(id=$id, uuid=$uuid, type=$type, name=$name, isDefault=$isDefault)"
}
