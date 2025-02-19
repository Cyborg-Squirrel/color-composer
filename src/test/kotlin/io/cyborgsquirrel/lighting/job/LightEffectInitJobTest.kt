package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.client_config.repository.H2LedStripGroupRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.kotest.core.spec.style.StringSpec
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest(startApplication = false, transactional = false)
class LightEffectInitJobTest(
    private val lightEffectRepository: H2LightEffectRepository,
    private val ledStripRepository: H2LedStripRepository,
    private val ledStripGroupRepository: H2LedStripGroupRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val objectMapper: ObjectMapper,
) : StringSpec({

    "Init light effects - happy path" {
//        val job = LightEffectInitJob(
//            lightEffectRepository,
//            lightEffectLedStripAssociationRepository,
//            ledStripRepository,
//            ledStripGroupRepository,
//            activeLightEffectRegistry,
//            objectMapper,
//        )
//
//        lightEffectRepository.save(LightEffectEntity())
//        job.run()
    }
})