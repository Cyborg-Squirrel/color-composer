package io.cyborgsquirrel.lighting.effect_trigger.triggers

import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.AnimatedSpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.repository.ActiveLightEffectRepository
import io.cyborgsquirrel.lighting.effects.repository.ActiveLightEffectRepositoryImpl
import io.cyborgsquirrel.lighting.effects.settings.SpectrumLightEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.model.strip.LedStripModel
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelper
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelperImpl
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import java.time.*
import java.util.*
import kotlin.jvm.optionals.getOrNull

@MicronautTest(startApplication = false, transactional = false)
class TimeTriggerTest(
    private val timeHelper: TimeHelper,
    private val activeLightEffectRepository: ActiveLightEffectRepository,
) : BehaviorSpec({
    lateinit var mockTimeHelper: TimeHelper
    lateinit var activeEffect: ActiveLightEffect
    lateinit var mockActiveLightEffectRepository: ActiveLightEffectRepository

    beforeTest {
        mockTimeHelper = getMock(timeHelper)
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
        val trigger = TimeTrigger(mockTimeHelper, settings, activeEffect.uuid)
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

    @MockBean(ActiveLightEffectRepositoryImpl::class)
    fun activeEffectRepository(): ActiveLightEffectRepository {
        return mockk()
    }
}