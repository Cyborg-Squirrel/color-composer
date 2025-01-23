package io.cyborgsquirrel.lighting.effect_trigger

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeTrigger
import io.cyborgsquirrel.repository.H2LocationConfigRepository
import io.cyborgsquirrel.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelper
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelperImpl
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

@MicronautTest(startApplication = false, transactional = false)
class TimeTriggerTest(private val timeHelper: TimeHelper) : BehaviorSpec({
    lateinit var mockTimeHelper: TimeHelper

    beforeTest {
        mockTimeHelper = getMock(timeHelper)
    }

    fun mockResponses(date: LocalDate, time: LocalTime) {
        every {
            mockTimeHelper.now()
        } returns LocalDateTime.of(date, time)
    }

    given("A TimeTrigger") {
        val date = LocalDate.of(2025, 1, 21)
        val time = LocalTime.of(17, 1)
        mockResponses(date, time)
        val settings = TimeTriggerSettings(
            LocalTime.of(17, 0),
            Duration.ofMinutes(30),
            Int.MAX_VALUE,
            TriggerType.StartEffect
        )
        val trigger = TimeTrigger(mockTimeHelper, settings)
        and("The trigger condition is met") {
            `when`("The latest trigger activation is requested") {
                val activationOptional = trigger.lastActivation()
                then("The last activation shall be returned") {
                    val activation = activationOptional.getOrNull()
                    activation shouldNotBe null
                    activation!!.sequenceNumber shouldBe 1
                    activation.timestamp.toLocalDate() shouldBe date
                    activation.timestamp.toLocalTime() shouldBe settings.triggerTime
                    activation.settings shouldBe settings
                }
            }
        }
    }
}) {
    @MockBean(TimeHelperImpl::class)
    fun timeHelper(): TimeHelper {
        return mockk()
    }
}