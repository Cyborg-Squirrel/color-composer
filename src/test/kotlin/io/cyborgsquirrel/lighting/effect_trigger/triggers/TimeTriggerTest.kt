package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.SpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectServiceImpl
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.util.time.TimeHelper
import io.cyborgsquirrel.util.time.TimeHelperImpl
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

@MicronautTest(startApplication = false, transactional = false)
class TimeTriggerTest(
    private val timeHelper: TimeHelper,
    private val activeLightEffectService: ActiveLightEffectService,
) : BehaviorSpec({
    lateinit var mockTimeHelper: TimeHelper
    lateinit var activeEffect: ActiveLightEffect
    lateinit var mockActiveLightEffectService: ActiveLightEffectService

    beforeTest {
        mockTimeHelper = getMock(timeHelper)
        mockActiveLightEffectService = getMock(activeLightEffectService)

        val mockStrip = mockk<SingleLedStripModel>()
        val effect = SpectrumLightEffect(60, SpectrumEffectSettings.default(60), null)
        activeEffect = ActiveLightEffect(
            UUID.randomUUID().toString(),
            1,
            true,
            LightEffectStatus.Idle,
            effect,
            listOf(),
            mockStrip,
        )

        every {
            mockActiveLightEffectService.getEffectWithUuid(activeEffect.effectUuid)
        } returns Optional.of(activeEffect)
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
            LocalTime.of(17, 0), null, Duration.ofMinutes(30), Int.MAX_VALUE, TriggerType.StartEffect
        )
        val trigger = TimeTrigger(mockTimeHelper, settings, UUID.randomUUID().toString(), activeEffect.effectUuid)
        and("The trigger condition is met") {
            `when`("The latest trigger activation is requested") {
                val activationOptional = trigger.lastActivation()
                then("The last activation shall be returned") {
                    val activation = activationOptional.getOrNull()
                    activation shouldNotBe null
                    activation!!.activationNumber shouldBe 1
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

    @MockBean(ActiveLightEffectServiceImpl::class)
    fun activeEffectRepository(): ActiveLightEffectService {
        return mockk()
    }
}