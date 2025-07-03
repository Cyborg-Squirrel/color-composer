package io.cyborgsquirrel.led_strips.service

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.job.WebsocketJobManager
import io.cyborgsquirrel.lighting.limits.PowerLimiterService
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class LedStripApiService(
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val stripRepository: H2LedStripRepository,
    private val clientRepository: H2LedStripClientRepository,
    private val limitService: PowerLimiterService,
    private val websocketJobManager: WebsocketJobManager,
) {

    fun createStrip(request: CreateLedStripRequest): String {
        val clientEntityOptional = clientRepository.findByUuid(request.clientUuid)
        if (clientEntityOptional.isPresent) {
            val stripEntity = LedStripEntity(
                client = clientEntityOptional.get(),
                uuid = UUID.randomUUID().toString(),
                name = request.name,
                pin = request.pin,
                length = request.length,
                height = request.height ?: 1,
                powerLimit = request.powerLimit,
                blendMode = request.blendMode ?: BlendMode.Additive
            )
            stripRepository.save(stripEntity)
            if (stripEntity.powerLimit != null) limitService.setLimit(stripEntity.uuid!!, stripEntity.powerLimit!!)
            return stripEntity.uuid!!
        } else {
            throw ClientRequestException("No client exists with uuid ${request.clientUuid}!")
        }
    }

    fun updateStrip(uuid: String, updatedStrip: UpdateLedStripRequest) {
        val entityOptional = stripRepository.findByUuid(uuid)
        if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            // TODO notify any running effects of the updated length or height. Force user to restart effects?
            // TODO notify renderer or active effect registry of blend mode changes.
            val newEntity = entity.copy(
                name = updatedStrip.name ?: entity.name,
                pin = updatedStrip.pin ?: entity.pin,
                length = updatedStrip.length ?: entity.length,
                height = updatedStrip.height ?: entity.height,
                powerLimit = updatedStrip.powerLimit ?: entity.powerLimit,
                blendMode = updatedStrip.blendMode ?: entity.blendMode
            )

            if (newEntity != entity) {
                val newStripEntity = stripRepository.update(newEntity)

                if (updatedStrip.powerLimit != null) limitService.setLimit(uuid, updatedStrip.powerLimit)

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
                        )
                    )
                    if (it != newEffect) {
                        activeLightEffectRegistry.addOrUpdateEffect(it)
                    }
                }
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
                websocketJobManager.updateJob(entity.client!!)
            } else {
                throw ClientRequestException("Could not delete strip with uuid $uuid. Please delete or reassign its effects and group memberships first.")
            }
        } else {
            throw ClientRequestException("Could not delete strip with uuid $uuid. It does not exist.")
        }
    }
}