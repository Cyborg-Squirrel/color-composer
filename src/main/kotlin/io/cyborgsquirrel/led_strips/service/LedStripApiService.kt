package io.cyborgsquirrel.led_strips.service

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.power_limits.PowerLimiterService
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class LedStripApiService(
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val stripRepository: H2LedStripRepository,
    private val clientRepository: H2LedStripClientRepository,
    private val limitService: PowerLimiterService,
    private val streamJobManager: StreamJobManager,
) {

    private val validPiPins = listOf("D10", "D12", "D18", "D21")

    fun createStrip(request: CreateLedStripRequest): String {
        val clientEntityOptional = clientRepository.findByUuid(request.clientUuid)
        if (clientEntityOptional.isPresent) {
            val clientEntity = clientEntityOptional.get()
            validatePin(clientEntity, request.pin)
            var stripEntity = LedStripEntity(
                client = clientEntity,
                uuid = UUID.randomUUID().toString(),
                name = request.name,
                pin = request.pin,
                length = request.length,
                height = request.height ?: 1,
                powerLimit = request.powerLimit,
                brightness = request.brightness,
                blendMode = request.blendMode ?: BlendMode.Additive
            )

            if (request.brightness == null) {
                val brightness = limitService.getDefaultBrightness(stripEntity)
                stripEntity = stripEntity.copy(brightness = brightness)
            }

            stripRepository.save(stripEntity)
            if (stripEntity.powerLimit != null) limitService.setLimit(stripEntity.uuid!!, stripEntity.powerLimit!!)

            return stripEntity.uuid!!
        } else {
            throw ClientRequestException("No client exists with uuid ${request.clientUuid}!")
        }
    }

    fun updateStrip(uuid: String, request: UpdateLedStripRequest) {
        val entityOptional = stripRepository.findByUuid(uuid)
        if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            if (request.pin != null) {
                validatePin(entity.client!!, request.pin)
            }

            // TODO notify any running effects of the updated length or height. Force user to restart effects?
            // TODO notify renderer or active effect registry of blend mode changes.
            val newEntity = entity.copy(
                name = request.name ?: entity.name,
                pin = request.pin ?: entity.pin,
                length = request.length ?: entity.length,
                height = request.height ?: entity.height,
                powerLimit = request.powerLimit ?: entity.powerLimit,
                blendMode = request.blendMode ?: entity.blendMode,
                brightness = request.brightness ?: entity.brightness,
            )

            if (newEntity != entity) {
                val newStripEntity = stripRepository.update(newEntity)

                if (request.powerLimit != null) limitService.setLimit(uuid, request.powerLimit)

                val effects = activeLightEffectRegistry.getAllEffectsForStrip(uuid)
                effects.forEach {
                    val newEffect = it.copy(
                        strip = LedStripModel(
                            newStripEntity.name!!,
                            newStripEntity.uuid!!,
                            newStripEntity.pin!!,
                            newStripEntity.length!!,
                            newStripEntity.height,
                            newStripEntity.blendMode!!,
                            newStripEntity.brightness!!,
                        )
                    )
                    if (it != newEffect) {
                        activeLightEffectRegistry.addOrUpdateEffect(it)
                    }
                }

                streamJobManager.notifyJobOfDataUpdate(entity.client!!)
            }
        } else {
            throw ClientRequestException("Client with uuid $uuid does not exist! Please create it first before updating it.")
        }
    }

    fun onStripDeleted(uuid: String) {
        val entityOptional = stripRepository.findByUuid(uuid)
        if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            if (entity.effects.isEmpty() && entity.members.isEmpty()) {
                stripRepository.deleteById(entity.id)
                limitService.removeLimit(uuid)
                val effects = activeLightEffectRegistry.getAllEffectsForStrip(uuid)
                effects.forEach {
                    activeLightEffectRegistry.removeEffect(it)
                }
                streamJobManager.notifyJobOfDataUpdate(entity.client!!)
            } else {
                throw ClientRequestException("Could not delete strip with uuid $uuid. Please delete or reassign its effects and group memberships first.")
            }
        } else {
            throw ClientRequestException("Could not delete strip with uuid $uuid. It does not exist.")
        }
    }

    private fun nightDriverPinValid(pin: String): Boolean {
        val regex = Regex("\\d+")
        val matches = regex.find(pin)

        return if (matches?.groups?.isNotEmpty() == true) {
            // NightDriver supports 16 channels (pins) starting at 1
            pin.toInt() in 1..16
        } else {
            false
        }
    }

    private fun validatePin(clientEntity: LedStripClientEntity, pin: String) {
        val isPinValid = when (clientEntity.clientType) {
            ClientType.Pi -> validPiPins.contains(pin)
            ClientType.NightDriver -> nightDriverPinValid(pin)
            null -> throw Exception("No client type specified for client ${clientEntity.uuid}")
        }

        if (!isPinValid) {
            throw ClientRequestException("Pin $pin is not a valid pin. Must be a channel (1-16) for a NightDriver client or D10, D12, D18, or D21 for a Pi client.")
        }

        if (clientEntity.strips.map { it.pin }.contains(pin)) {
            throw ClientRequestException("Pin $pin is already used by a LED strip connected to client ${clientEntity.uuid}.")
        }
    }
}