package io.cyborgsquirrel.lighting.triggers

import io.cyborgsquirrel.entity.LocationConfigEntity
import io.cyborgsquirrel.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.lighting.enums.TriggerType
import io.cyborgsquirrel.repository.H2LocationConfigRepository
import io.cyborgsquirrel.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelper
import java.time.LocalDateTime
import java.util.*
import io.cyborgsquirrel.util.ymd
import io.micronaut.serde.ObjectMapper
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

class SunriseSunsetTrigger(
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
    triggerType: TriggerType,
) : LightEffectTrigger(triggerType) {

    private var todayEntity: SunriseSunsetTimeEntity? = null
    private var locationEntity: LocationConfigEntity? = null

    override fun init() {
        refresh()
    }

    // TODO Replace placeholder logic after refresh() with configurable logic (x minutes before/after sunrise, sunset, etc)
    override fun lastActivation(): Optional<LocalDateTime> {
        refresh()

        if (todayEntity != null) {
            val todaySunriseSunsetData = objectMapper.readValue(todayEntity!!.json, SunriseSunsetModel::class.java)
            val todaySunriseTime =
                timeHelper.utcTimestampToZoneDateTime(todaySunriseSunsetData.results.sunrise)
                    .toLocalDateTime()
            return Optional.of(todaySunriseTime)
        }

        return Optional.empty()
    }

    override fun triggerThreshold(): Duration {
        return Duration.ofMinutes(5)
    }

    private fun refresh() {
        val today = timeHelper.today()
        val todayYmd = today.ymd()
        val locationEntityOptional = locationConfigRepository.findByActiveTrue()

        if (locationEntityOptional.isPresent) {
            val locationChanged = locationEntity?.id != locationEntityOptional.get().id
            val todayEntityUpToDate = todayEntity != null && todayEntity?.ymd == todayYmd

            if (locationChanged || !todayEntityUpToDate) {
                locationEntity = locationEntityOptional.get()
                val todayEntityOptional =
                    sunriseSunsetTimeRepository.findByYmdEqualsAndLocationEquals(todayYmd, locationEntity!!)
                todayEntity = todayEntityOptional.getOrNull()
            }
        }
    }
}