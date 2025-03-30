package io.cyborgsquirrel.sunrise_sunset.entity

import io.micronaut.data.annotation.*
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@MappedEntity("location_configs")
data class LocationConfigEntity (
    @Id
    @GeneratedValue
    var id: Long = -1,

    @MappedProperty("lat")
    var latitude: String? = null,

    @MappedProperty("lng")
    var longitude: String? = null,

    var active: Boolean? = null,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "location")
    var sunriseSunsetTimes: Set<SunriseSunsetTimeEntity> = setOf(),
)