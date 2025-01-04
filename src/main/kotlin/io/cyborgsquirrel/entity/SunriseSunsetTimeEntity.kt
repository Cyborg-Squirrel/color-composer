package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("sunrise_sunset_times")
data class SunriseSunsetTimeEntity(
    @Id
    @GeneratedValue
    var id: Long = -1,

    var ymd: String? = null,

    var json: String? = null,

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var location: LocationConfigEntity? = null,
)