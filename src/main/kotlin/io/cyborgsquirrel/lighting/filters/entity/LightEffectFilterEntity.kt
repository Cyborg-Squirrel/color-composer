package io.cyborgsquirrel.lighting.filters.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType

@MappedEntity("light_effect_filters")
data class LightEffectFilterEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "filter")
    var effectJunctions: Set<LightEffectFilterJunctionEntity> = setOf(),

    var uuid: String? = null,

    @TypeDef(type = DataType.JSON)
    var settings: Map<String, Any>?,

    var type: String? = null,

    var name: String? = null,
)