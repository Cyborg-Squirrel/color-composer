package io.cyborgsquirrel.lighting.filters.entity

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("light_effect_filters")
data class LightEffectFilterEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var effect: LightEffectEntity? = null,

    var uuid: String? = null,

    @TypeDef(type = DataType.JSON)
    var settings: Map<String, Any>?,

    var name: String? = null,
)