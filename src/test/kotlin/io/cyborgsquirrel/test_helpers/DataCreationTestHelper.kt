package io.cyborgsquirrel.test_helpers

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.serde.ObjectMapper
import java.util.*

fun createLedStripClientEntity(
    clientRepository: H2LedStripClientRepository,
    name: String,
    address: String,
    apiPort: Int,
    wsPort: Int
): LedStripClientEntity =
    clientRepository.save(
        LedStripClientEntity(
            name = name,
            address = address,
            uuid = UUID.randomUUID().toString(),
            apiPort = apiPort,
            wsPort = wsPort
        )
    )

fun saveLedStrips(
    stripRepository: H2LedStripRepository,
    client: LedStripClientEntity,
    strips: List<Pair<String, Int>>
): List<LedStripEntity> =
    strips.map { (name, length) ->
        stripRepository.save(
            LedStripEntity(
                client = client,
                name = name,
                uuid = UUID.randomUUID().toString(),
                length = length,
                blendMode = BlendMode.Average,
            )
        )
    }

fun saveLightEffect(
    effectRepository: H2LightEffectRepository,
    objectMapper: ObjectMapper,
    strip: LedStripEntity
): LightEffectEntity =
    effectRepository.save(
        LightEffectEntity(
            strip = strip,
            uuid = UUID.randomUUID().toString(),
            settings = objectToMap(objectMapper, SpectrumEffectSettings(strip.length!!, animated = false)),
            type = LightEffectConstants.SPECTRUM_NAME,
            name = "My light effect",
            status = LightEffectStatus.Created
        )
    )