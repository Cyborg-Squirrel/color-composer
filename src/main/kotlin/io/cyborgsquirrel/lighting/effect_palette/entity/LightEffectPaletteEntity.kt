package io.cyborgsquirrel.lighting.effect_palette.entity

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType

@MappedEntity("light_effect_palettes")
data class LightEffectPaletteEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "palette")
    var effects: Set<LightEffectEntity> = setOf(),

    var uuid: String? = null,

    @TypeDef(type = DataType.JSON)
    var settings: Map<String, Any>?,

    var type: String? = null,

    var name: String? = null,
)