package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.entity.LocationConfigEntity
import io.cyborgsquirrel.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.settings.SunriseSunsetTriggerSettings
import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeHelper
import io.cyborgsquirrel.util.time.ymd
import io.micronaut.serde.ObjectMapper
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

class SunriseSunsetTrigger(
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
    settings: SunriseSunsetTriggerSettings,
    activeEffectUuid: String,
) : LightEffectTrigger(settings, activeEffectUuid) {

    private var todayEntity: SunriseSunsetTimeEntity? = null
    private var locationEntity: LocationConfigEntity? = null
    private var sequenceNumber = 0
    private var lastActivation: LocalDateTime? = null

    override fun lastActivation(): Optional<TriggerActivation> {
        refresh()

        if (todayEntity != null) {
            val todaySunriseSunsetData = objectMapper.readValue(todayEntity!!.json, SunriseSunsetModel::class.java)
            val triggerTime = getTimestampForOption(todaySunriseSunsetData, getSunriseSunsetOption())
            val now = timeHelper.now()

            if (triggerTime.toLocalDate() != lastActivation?.toLocalDate() && now.isAfter(triggerTime)) {
                sequenceNumber++
                lastActivation = triggerTime
            }
        }

        return if (lastActivation == null) Optional.empty() else Optional.of(
            TriggerActivation(
                lastActivation!!,
                settings,
                sequenceNumber,
            )
        )
    }

    private fun getSunriseSunsetOption(): SunriseSunsetOption {
        return (settings as SunriseSunsetTriggerSettings).sunriseSunsetOption
    }

    private fun getTimestampForOption(
        todaySunriseSunsetData: SunriseSunsetModel,
        sunriseSunsetOption: SunriseSunsetOption
    ): LocalDateTime {
        val timestampString = when (sunriseSunsetOption) {
            SunriseSunsetOption.Sunrise -> todaySunriseSunsetData.results.sunrise
            SunriseSunsetOption.Sunset -> todaySunriseSunsetData.results.sunset
        }

        return timeHelper.utcTimestampToZoneDateTime(timestampString)
            .toLocalDateTime()
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