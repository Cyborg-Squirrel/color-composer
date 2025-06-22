package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeOfDayTriggerSettings
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.SpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistryImpl
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.sunrise_sunset.entity.LocationConfigEntity
import io.cyborgsquirrel.sunrise_sunset.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.cyborgsquirrel.sunrise_sunset.job.SunriseSunsetApiTestData.Companion.apiResponse2025Jan21Json
import io.cyborgsquirrel.sunrise_sunset.job.SunriseSunsetApiTestData.Companion.apiResponse2025Jan2Json
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.*
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
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
) : StringSpec({
    lateinit var mockTimeHelper: TimeHelper
    lateinit var mockLocationConfigRepository: H2LocationConfigRepository
    lateinit var mockSunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository
    lateinit var mockActiveLightEffectRegistry: ActiveLightEffectRegistry
    lateinit var timeOfDayService: TimeOfDayService

    // Location option 1
    val duluthMnLocation = LocationConfigEntity(1, "46.465978", "-92.062368", true)
    val cst = ZoneId.of("America/Chicago")
    // Location option 2
    val londonGbLocation = LocationConfigEntity(2, "51.3026", "7.39", active = true)
    val utc = ZoneId.of("UTC")

    lateinit var activeEffect: ActiveLightEffect

    beforeTest {
        mockTimeHelper = getMock(timeHelper)
        timeOfDayService = TimeOfDayServiceImpl(mockTimeHelper)
        mockLocationConfigRepository = getMock(locationConfigRepository)
        mockSunriseSunsetTimeRepository = getMock(sunriseSunsetTimeRepository)
        mockActiveLightEffectRegistry = getMock(activeLightEffectRegistry)

        val mockStrip = mockk<LedStripModel>()
        val effect = SpectrumLightEffect(60, SpectrumEffectSettings.default(60), null)
        activeEffect = ActiveLightEffect(
            UUID.randomUUID().toString(),
            1,
            true,
            LightEffectStatus.Idle,
            effect,
            mockStrip,
            listOf()
        )

        every {
            mockActiveLightEffectRegistry.findEffectsWithStatus(LightEffectStatus.Playing)
        } returns listOf(activeEffect)
        every {
            mockActiveLightEffectRegistry.getEffectWithUuid(activeEffect.effectUuid)
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
            mockSunriseSunsetTimeRepository.findByYmdEqualsAndLocation(date.ymd(), locationConfig)
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

        val settings = TimeOfDayTriggerSettings(
            TimeOfDay.Sunrise,
            Duration.ofMinutes(30),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            TimeOfDayTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                timeOfDayService,
                settings,
                UUID.randomUUID().toString(),
                activeEffect.effectUuid
            )

        val activationOptional = trigger.lastActivation()
        val activation = activationOptional.getOrNull()
        activation shouldNotBe null
        activation!!.activationNumber shouldBe 1
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
        val settings = TimeOfDayTriggerSettings(
            TimeOfDay.Sunset,
            Duration.ofHours(1),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            TimeOfDayTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                timeOfDayService,
                settings,
                UUID.randomUUID().toString(),
                activeEffect.effectUuid
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
        val settings = TimeOfDayTriggerSettings(
            TimeOfDay.Sunset,
            Duration.ofHours(1),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            TimeOfDayTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                timeOfDayService,
                settings,
                UUID.randomUUID().toString(),
                activeEffect.effectUuid
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
            mockSunriseSunsetTimeRepository.findByYmdEqualsAndLocation(date.ymd(), duluthMnLocation)
        } returns Optional.empty()

        mockTime(date, time)
        mockTimestampParse(utc)
        val settings = TimeOfDayTriggerSettings(
            TimeOfDay.Sunset,
            Duration.ofHours(1),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger =
            TimeOfDayTrigger(
                mockSunriseSunsetTimeRepository,
                mockLocationConfigRepository,
                objectMapper,
                mockTimeHelper,
                timeOfDayService,
                settings,
                UUID.randomUUID().toString(),
                activeEffect.effectUuid
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

    @MockBean(ActiveLightEffectRegistryImpl::class)
    fun activeEffectRepository(): ActiveLightEffectRegistry {
        return mockk()
    }
}