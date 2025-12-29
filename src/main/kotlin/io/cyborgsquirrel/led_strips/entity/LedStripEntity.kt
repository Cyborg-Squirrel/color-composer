package io.cyborgsquirrel.led_strips.entity

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@MappedEntity("led_strips")
data class LedStripEntity(

    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var client: LedStripClientEntity? = null,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "strip")
    var members: Set<GroupMemberLedStripEntity> = setOf(),

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "strip")
    var effects: Set<LightEffectEntity> = setOf(),

    var uuid: String? = null,

    var name: String? = null,

    // Raspberry Pi pin or NightDriver channel
    var pin: String? = null,

    var length: Int? = null,

    // Always 1 for strips, can be greater than 1 for matrices
    var height: Int = 1,

    var brightness: Int? = null,

    @MappedEntity("blend_mode")
    @Enumerated(EnumType.STRING)
    var blendMode: BlendMode? = null,
)