package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.sunrise_sunset.entity.LocationConfigEntity
import io.cyborgsquirrel.sunrise_sunset.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.lighting.effect_trigger.model.TriggerActivation
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeOfDayTriggerSettings
import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeOfDayService
import io.cyborgsquirrel.util.time.TimeHelper
import io.cyborgsquirrel.util.time.ymd
import io.micronaut.serde.ObjectMapper
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

class TimeOfDayTrigger(
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
    private val timeOfDayService: TimeOfDayService,
    settings: TimeOfDayTriggerSettings,
    uuid: String,
    effectUuid: String,
) : LightEffectTrigger(settings, uuid, effectUuid) {

    private var todayEntity: SunriseSunsetTimeEntity? = null
    private var locationEntity: LocationConfigEntity? = null
    private var sequenceNumber = 0
    private var lastActivation: LocalDateTime? = null

    override fun lastActivation(): Optional<TriggerActivation> {
        refresh()

        if (todayEntity != null) {
            val todaySunriseSunsetData = objectMapper.readValue(todayEntity!!.json, SunriseSunsetModel::class.java)
            val triggerTime =
                timeOfDayService.timeOfDayToLocalDateTime(todaySunriseSunsetData, getSunriseSunsetOption())
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

    private fun getSunriseSunsetOption(): TimeOfDay {
        return (settings as TimeOfDayTriggerSettings).timeOfDay
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
                    sunriseSunsetTimeRepository.findByYmdEqualsAndLocation(todayYmd, locationEntity!!)
                todayEntity = todayEntityOptional.getOrNull()
            }
        }
    }
}