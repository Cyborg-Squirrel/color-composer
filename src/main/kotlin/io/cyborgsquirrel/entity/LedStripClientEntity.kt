package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("led_strip_clients")
data class LedStripClientEntity(

    @Id
    @GeneratedValue
    var id: Int = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "client")
    var strips: Set<LedStripEntity> = setOf(),

    var name: String?,

    var address: String?,

    var wsPort: Int? = null,

    var apiPort: Int? = null,
)