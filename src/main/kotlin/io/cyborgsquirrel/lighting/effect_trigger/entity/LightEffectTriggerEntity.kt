package io.cyborgsquirrel.lighting.effect_trigger.entity

import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("light_effect_triggers")
data class LightEffectTriggerEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var effect: LightEffectEntity? = null,

    var uuid: String? = null,

    @TypeDef(type = DataType.JSON)
    var settings: Map<String, Any>?,

    var type: String? = null,

    var name: String? = null,
)