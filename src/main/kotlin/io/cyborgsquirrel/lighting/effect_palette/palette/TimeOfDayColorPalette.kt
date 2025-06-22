package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.helper.TimePaletteHelper
import io.cyborgsquirrel.lighting.effect_palette.settings.SettingsPalette
import io.cyborgsquirrel.lighting.effect_palette.settings.TimeOfDayPaletteSettings
import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeHelper
import io.cyborgsquirrel.util.time.TimeOfDayService
import io.cyborgsquirrel.util.time.localDateFromYmd
import io.micronaut.serde.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime

class TimeOfDayColorPalette(
    private val settings: TimeOfDayPaletteSettings,
    private val timeHelper: TimeHelper,
    private val timeOfDayService: TimeOfDayService,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val objectMapper: ObjectMapper,
    uuid: String,
    strip: LedStrip,
) : ColorPalette(uuid, strip) {

    init {
        fetchLocationSunriseSunsetData()
    }

    private val helper = TimePaletteHelper()
    private var latestSunriseSunsetModel: SunriseSunsetModel? = null
    private var latestSunriseSunsetDate: LocalDate? = null

    override fun getPrimaryColor(index: Int): RgbColor {
        val now = timeHelper.now()
        if (isSunriseSunsetTimeExpired(now)) fetchLocationSunriseSunsetData()
        val points = getTimeColorMap()
        val palette = helper.getPalette(now, points)
        return palette.primaryColor
    }

    override fun getSecondaryColor(index: Int): RgbColor {
        val now = timeHelper.now()
        if (isSunriseSunsetTimeExpired(now)) fetchLocationSunriseSunsetData()
        val points = getTimeColorMap()
        val palette = helper.getPalette(now, points)
        return palette.secondaryColor
    }

    override fun getTertiaryColor(index: Int): RgbColor? {
        val now = timeHelper.now()
        if (isSunriseSunsetTimeExpired(now)) fetchLocationSunriseSunsetData()
        val points = getTimeColorMap()
        val palette = helper.getPalette(now, points)
        return palette.tertiaryColor
    }

    override fun getOtherColors(index: Int): List<RgbColor> {
        val now = timeHelper.now()
        if (isSunriseSunsetTimeExpired(now)) fetchLocationSunriseSunsetData()
        val points = getTimeColorMap()
        val palette = helper.getPalette(now, points)
        return palette.otherColors
    }

    private fun isSunriseSunsetTimeExpired(now: LocalDateTime): Boolean {
        return if (latestSunriseSunsetDate == null) true else latestSunriseSunsetDate!!.plusDays(30)
            .isBefore(now.toLocalDate())
    }

    private fun fetchLocationSunriseSunsetData() {
        val activeLocationOptional = locationConfigRepository.findByActiveTrue()
        if (activeLocationOptional.isPresent) {
            val location = activeLocationOptional.get()
            val sunriseSunsetTimes = sunriseSunsetTimeRepository.findByLocationOrderByYmd(location)
            if (sunriseSunsetTimes.isNotEmpty()) {
                val now = timeHelper.now()
                val latestSunriseSunsetTime = sunriseSunsetTimes.last()
                val latestSunriseSunsetTimeDate = localDateFromYmd(latestSunriseSunsetTime.ymd!!)

                // Use the sunrise/sunset entity if it's less than 30 days old
                if (now.toLocalDate().minusDays(30).isBefore(latestSunriseSunsetTimeDate)) {
                    val latestSunriseSunsetTimeEntity = sunriseSunsetTimes.last()
                    latestSunriseSunsetModel =
                        objectMapper.readValue(latestSunriseSunsetTimeEntity.json, SunriseSunsetModel::class.java)
                    latestSunriseSunsetDate = latestSunriseSunsetTimeDate
                }
            }
        }
    }

    private fun getTimeColorMap(): Map<LocalDateTime, SettingsPalette> {
        return if (latestSunriseSunsetModel != null) {
            settings.paletteMap.mapKeys {
                timeOfDayService.timeOfDayToLocalDateTime(latestSunriseSunsetModel!!, it.key)
            }
        } else {
            settings.paletteMap.mapKeys {
                val now = timeHelper.now()
                val defaultTime = timeOfDayService.defaultTimeOf(it.key)
                now.withHour(defaultTime.hour).withMinute(defaultTime.minute)
            }
        }
    }
}