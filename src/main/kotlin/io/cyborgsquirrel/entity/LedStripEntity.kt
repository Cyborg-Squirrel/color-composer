package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id

@Serdeable
@MappedEntity("led_strips")
data class LedStripEntity(

    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var client: LedStripClientEntity? = null,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "strip")
    var groupMemberStrips: Set<GroupMemberLedStripEntity> = setOf(),

    var uuid: String? = null,

    var name: String? = null,

    var length: Int? = null,

    // Always 1 for strips, can be greater than 1 for matrices
    var height: Int = 1,
)