package io.cyborgsquirrel.entity

import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Serdeable
@MappedEntity("light_effects")
data class LightEffectEntity(

    @Id
    @GeneratedValue
    var id: Long = -1,

    @TypeDef(type = DataType.JSON)
    var settings: Map<String, Any>?,

    @Enumerated(EnumType.STRING)
    var status: LightEffectStatus?,

    var name: String?
)