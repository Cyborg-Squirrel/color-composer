package io.cyborgsquirrel.led_strips.service

import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.limits.PowerLimiterService
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton

@Singleton
class LedStripSetupService(
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val stripRepository: H2LedStripRepository,
    private val limitService: PowerLimiterService
) {

    fun onStripUpdate(uuid: String, updatedStrip: UpdateLedStripRequest) {
        val entityOptional = stripRepository.findByUuid(uuid)
        if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            // TODO notify any running effects of the updated length or height. Force user to restart effects?
            // TODO notify renderer or active effect registry of blend mode changes.
            val newEntity = entity.copy(
                name = updatedStrip.name ?: entity.name,
                length = updatedStrip.length ?: entity.length,
                height = updatedStrip.height ?: entity.height,
                powerLimit = updatedStrip.powerLimit ?: entity.powerLimit,
                blendMode = updatedStrip.blendMode ?: entity.blendMode
            )

            if (newEntity != entity) {
                val newStripEntity = stripRepository.update(newEntity)

                if (updatedStrip.powerLimit != null) limitService.setLimit(uuid, updatedStrip.powerLimit)

                val effects = activeLightEffectRegistry.findAllEffectsForStrip(uuid)
                effects.forEach {
                    val newEffect = it.copy(
                        strip = LedStripModel(
                            newStripEntity.name!!,
                            newStripEntity.uuid!!,
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
                val effects = activeLightEffectRegistry.findAllEffectsForStrip(uuid)
                effects.forEach {
                    activeLightEffectRegistry.removeEffect(it)
                }
            } else {
                // TODO cascading delete? Unassign effects and group membership?
                throw ClientRequestException("Could not delete strip with uuid $uuid. Please delete or reassign its effects and group memberships first.")
            }
        } else {
            throw ClientRequestException("Could not delete strip with uuid $uuid. It does not exist.")
        }
    }
}