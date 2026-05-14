package io.cyborgsquirrel.sunrise_sunset.repository

import io.cyborgsquirrel.sunrise_sunset.entity.LocationConfigEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import java.util.*

interface LocationConfigRepository : CrudRepository<LocationConfigEntity, Long> {
    @Join(value = "sunriseSunsetTimes", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<LocationConfigEntity>

    @Join(value = "sunriseSunsetTimes", type = Join.Type.LEFT_FETCH)
    fun findByActiveTrue(): Optional<LocationConfigEntity>
}
