package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.entity.LocationConfigEntity
import io.cyborgsquirrel.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.lighting.effect_trigger.enums.SunriseSunsetOption
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.SunriseSunsetTriggerSettings
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.AnimatedSpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.repository.ActiveLightEffectRepository
import io.cyborgsquirrel.lighting.effects.repository.ActiveLightEffectRepositoryImpl
import io.cyborgsquirrel.lighting.effects.settings.SpectrumLightEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.model.strip.LedStripModel
import io.cyborgsquirrel.repository.H2LocationConfigRepository
import io.cyborgsquirrel.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.sunrise_sunset.job.SunriseSunsetApiTestData.Companion.apiResponse2025Jan21Json
import io.cyborgsquirrel.sunrise_sunset.job.SunriseSunsetApiTestData.Companion.apiResponse2025Jan2Json
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelper
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelperImpl
import io.cyborgsquirrel.util.ymd
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.jvm.optionals.getOrNull

@MicronautTest(startApplication = false, transactional = false)
class SunriseSunsetTriggerTest(
    private val locationConfigRepository: H2LocationConfigRepository,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
    private val activeLightEffectRepository: ActiveLightEffectRepository,
) : StringSpec({
    lateinit var mockTimeHelper: TimeHelper
    lateinit var mockLocationConfigRepository: H2LocationConfigRepository
    lateinit var mockSunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository
    lateinit var mockActiveLightEffectRepository: ActiveLightEffectRepository

    // Location option 1
    val duluthMnLocation = LocationConfigEntity(1, "46.465978", "-92.062368", true)
    val cst = ZoneId.of("America/Chicago")
    // Location option 2
    val londonGbLocation = LocationConfigEntity(2, "51.3026", "7.39", active = true)
    val utc = ZoneId.of("UTC")

    lateinit var activeEffect: ActiveLightEffect

    beforeTest {
        mockTimeHelper = getMock(timeHelper)
        mockLocationConfigRepository = getMock(locationConfigRepository)
        mockSunriseSunsetTimeRepository = getMock(sunriseSunsetTimeRepository)
        mockActiveLightEffectRepository = getMock(activeLightEffectRepository)

        val mockStrip = mockk<LedStripModel>()
        val effect = AnimatedSpectrumLightEffect(60, SpectrumLightEffectSettings(9))
        activeEffect = ActiveLightEffect(
            UUID.randomUUID().toString(), 1, LightEffectStatus.Created, effect, mockStrip, listOf()
        )

        every {
            mockActiveLightEffectRepository.findEffectsWithStatus(LightEffectStatus.Active)
        } returns listOf(activeEffect)
        every {
            mockActiveLightEffectRepository.findEffectWithUuid(activeEffect.uuid)
        } returns Optional.of(activeEffect)
    }

    fun mockTime(date: LocalDate, time: LocalTime) {
        every {
            mockTimeHelper.today()
        } returns date
        every {
            mockTimeHelper.now()
        } returns LocalDateTime.of(date, time)
    }

    fun mockLocation(date: LocalDate, apiResponseString: String, locationConfig: LocationConfigEntity) {
        every {
            mockLocationConfigRepository.findByActiveTrue()
        } returns Optional.of(locationConfig)
        every {
            mockSunriseSunsetTimeRepository.findByYmdEqualsAndLocationEquals(date.ymd(), locationConfig)
        } returns Optional.of(SunriseSunsetTimeEntity(1, date.ymd(), apiResponseString, locationConfig))
    }

    fun mockTimestampParse(zoneId: ZoneId) {
        every {
            mockTimeHelper.utcTimestampToZoneDateTime(any())
        } answers {
            val dateString = it.invocation.args.first() as String
            ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(
                zoneId
            )
        }
    }

    "SunriseSunsetTrigger happy path activation" {
        val date = LocalDate.of(2025, 1, 21)
        // Duluth, MN has a sunrise time approx 7:41 am local time
        val time = LocalTime.of(7, 45)
        mockTime(date, time)
        mockLocation(date, apiResponse2025Jan21Json, duluthMnLocation)
        mockTimestampParse(cst)

        val settings = SunriseSunsetTriggerSettings(
            SunriseSunsetOption.Sunrise,
            Duration.ofMinutes(30),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            SunriseSunsetTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                settings,
                activeEffect.uuid
            )

        val activationOptional = trigger.lastActivation()
        val activation = activationOptional.getOrNull()
        activation shouldNotBe null
        activation!!.sequenceNumber shouldBe 1
        activation.timestamp.toLocalDate() shouldBe date
        activation.timestamp.toLocalTime().hour shouldBe 7
        activation.timestamp.toLocalTime().minute shouldBe 41
        activation.settings shouldBe settings
    }

    "SunriseSunsetTrigger happy path two activations hours apart" {
        val date = LocalDate.of(2025, 1, 2)
        // Both times are after sunset
        val firstTime = LocalTime.of(15, 40)
        val secondTime = LocalTime.of(18, 40)
        var nowCalls = 0

        every {
            mockTimeHelper.today()
        } returns date
        every {
            mockTimeHelper.now()
        } answers {
            nowCalls++
            if (nowCalls == 1) LocalDateTime.of(date, firstTime) else LocalDateTime.of(date, secondTime)
        }

        mockLocation(date, apiResponse2025Jan2Json, londonGbLocation)
        mockTimestampParse(utc)
        val settings = SunriseSunsetTriggerSettings(
            SunriseSunsetOption.Sunset,
            Duration.ofHours(1),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            SunriseSunsetTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                settings,
                activeEffect.uuid
            )
        val activationOptionalA = trigger.lastActivation()
        val activationOptionalB = trigger.lastActivation()
        val activationA = activationOptionalA.get()
        val activationB = activationOptionalB.get()
        activationA shouldBe activationB
    }

    "SunriseSunsetTrigger no activation - trigger time not yet passed" {
        val date = LocalDate.of(2025, 1, 2)
        // Time is before sunset
        val time = LocalTime.of(12, 0)

        mockTime(date, time)
        mockLocation(date, apiResponse2025Jan2Json, londonGbLocation)
        mockTimestampParse(utc)
        val settings = SunriseSunsetTriggerSettings(
            SunriseSunsetOption.Sunset,
            Duration.ofHours(1),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            SunriseSunsetTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                settings,
                activeEffect.uuid
            )
        val activationOptional = trigger.lastActivation()
        activationOptional.isEmpty shouldBe true
    }

    "SunriseSunsetTrigger no activation - sunrise/sunset data missing" {
        val date = LocalDate.of(2025, 1, 2)
        // Time is after sunset
        val time = LocalTime.of(23, 0)

        every {
            mockLocationConfigRepository.findByActiveTrue()
        } returns Optional.of(duluthMnLocation)
        every {
            mockSunriseSunsetTimeRepository.findByYmdEqualsAndLocationEquals(date.ymd(), duluthMnLocation)
        } returns Optional.empty()

        mockTime(date, time)
        mockTimestampParse(utc)
        val settings = SunriseSunsetTriggerSettings(
            SunriseSunsetOption.Sunset,
            Duration.ofHours(1),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            SunriseSunsetTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                settings,
                activeEffect.uuid
            )
        val activationOptional = trigger.lastActivation()
        activationOptional.isEmpty shouldBe true
    }
}) {
    @MockBean(H2SunriseSunsetTimeRepository::class)
    fun sunriseSunsetRepository(): H2SunriseSunsetTimeRepository {
        return mockk()
    }

    @MockBean(H2LocationConfigRepository::class)
    fun locationConfigRepository(): H2LocationConfigRepository {
        return mockk()
    }

    @MockBean(TimeHelperImpl::class)
    fun timeHelper(): TimeHelper {
        return mockk()
    }

    @MockBean(ActiveLightEffectRepositoryImpl::class)
    fun activeEffectRepository(): ActiveLightEffectRepository {
        return mockk()
    }
}