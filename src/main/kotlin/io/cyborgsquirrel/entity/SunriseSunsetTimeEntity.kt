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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SunriseSunsetTimeEntity) return false

        if (id != other.id) return false
        if (ymd != other.ymd) return false
        if (json != other.json) return false
        if (location?.id != other.location?.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = (ymd + json).hashCode()
        result = 31 * result + location?.id.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    override fun toString(): String {
        return "SunriseSunsetTimeEntity(ymd='$ymd', json=${json?.substring(0, 15)}, location=${location?.id}, id=$id)"
    }

}