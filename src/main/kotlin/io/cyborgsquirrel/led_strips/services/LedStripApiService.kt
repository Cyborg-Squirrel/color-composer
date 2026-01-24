package io.cyborgsquirrel.led_strips.services

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.status.ClientStatusService
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.led_strips.responses.GetLedStripResponse
import io.cyborgsquirrel.led_strips.responses.GetLedStripsResponse
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.BouncingBallLightEffect
import io.cyborgsquirrel.lighting.effects.CustomLightEffect
import io.cyborgsquirrel.lighting.effects.FlameLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.effects.MarqueeEffect
import io.cyborgsquirrel.lighting.effects.NightriderLightEffect
import io.cyborgsquirrel.lighting.effects.SparkleLightEffect
import io.cyborgsquirrel.lighting.effects.SpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.WaveLightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.isActive
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.time.TimeHelper
import jakarta.inject.Singleton
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Singleton
class LedStripApiService(
    private val activeLightEffectService: ActiveLightEffectService,
    private val stripRepository: H2LedStripRepository,
    private val clientRepository: H2LedStripClientRepository,
    private val clientStatusService: ClientStatusService,
    private val timeHelper: TimeHelper,
) {

    private val validPiPins = listOf("D10", "D12", "D18", "D21")
    private val defaultBrightness = 20

    fun getStrip(uuid: String): GetLedStripResponse {
        val entityOptional = stripRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            val activeEffects = activeLightEffectService.getAllEffects().filter { it.status.isActive() }
            val entity = entityOptional.get()
            val response = entity.let { s ->
                GetLedStripResponse(
                    clientUuid = s.client!!.uuid!!,
                    name = s.name!!,
                    uuid = s.uuid!!,
                    pin = s.pin!!,
                    length = s.length!!,
                    height = s.height,
                    brightness = s.brightness!!,
                    blendMode = s.blendMode!!,
                    activeEffects = activeEffects.filter { it.strip.uuid == s.uuid }.size,
                )
            }
            response
        } else {
            throw ClientRequestException("No LED strip exists with uuid $uuid!")
        }
    }

    fun getStrips(clientUuid: String?): GetLedStripsResponse {
        if (clientUuid != null) {
            val clientEntityOptional = clientRepository.findByUuid(clientUuid)
            return if (clientEntityOptional.isPresent) {
                val activeEffects = activeLightEffectService.getAllEffects().filter { it.status.isActive() }
                val clientEntity = clientEntityOptional.get()
                val clientStatusOptional = clientStatusService.getStatusForClient(clientEntity)
                val stripResponseList = clientEntity.strips.map { s ->
                    GetLedStripResponse(
                        clientUuid = clientUuid,
                        name = s.name!!,
                        uuid = s.uuid!!,
                        pin = s.pin!!,
                        length = s.length!!,
                        height = s.height,
                        brightness = s.brightness!!,
                        blendMode = s.blendMode!!,
                        activeEffects = getActiveEffectsForStrip(
                            clientStatusOptional.getOrNull()?.status, activeEffects, s.uuid
                        )
                    )
                }
                GetLedStripsResponse(stripResponseList)
            } else {
                throw ClientRequestException("No client exists with uuid $clientUuid!")
            }
        } else {
            val clientEntities = clientRepository.queryAll()
            val stripEntities = clientEntities.map { it.strips }.flatten()
            val stripResponseList = stripEntities.map { s ->
                GetLedStripResponse(
                    clientUuid = s.client!!.uuid!!,
                    name = s.name!!,
                    uuid = s.uuid!!,
                    pin = s.pin!!,
                    length = s.length!!,
                    height = s.height,
                    brightness = s.brightness!!,
                    blendMode = s.blendMode!!,
                    activeEffects = getActiveEffectsForStrip(
                        clientStatusService.getStatusForClient(s.client!!).getOrNull()?.status,
                        activeLightEffectService.getAllEffects().filter { it.status.isActive() },
                        s.uuid
                    ),
                )
            }

            return GetLedStripsResponse(stripResponseList)
        }
    }

    fun createStrip(request: CreateLedStripRequest): String {
        val clientEntityOptional = clientRepository.findByUuid(request.clientUuid)
        if (clientEntityOptional.isPresent) {
            val clientEntity = clientEntityOptional.get()
            validatePin(clientEntity, request.pin)

            // Pi clients can only have one LED strip
            if (clientEntity.clientType == ClientType.Pi && clientEntity.strips.isNotEmpty()) {
                throw ClientRequestException("Pi clients can only have one LED strip connected.")
            }

            val stripEntity = LedStripEntity(
                client = clientEntity,
                uuid = UUID.randomUUID().toString(),
                name = request.name,
                pin = request.pin,
                length = request.length,
                height = request.height ?: 1,
                brightness = request.brightness ?: defaultBrightness,
                blendMode = request.blendMode ?: BlendMode.Additive
            )
            stripRepository.save(stripEntity)
            return stripEntity.uuid!!
        } else {
            throw ClientRequestException("No client exists with uuid ${request.clientUuid}!")
        }
    }

    fun updateStrip(uuid: String, request: UpdateLedStripRequest) {
        val stripEntityOptional = stripRepository.findByUuid(uuid)
        if (stripEntityOptional.isPresent) {
            val stripEntity = stripEntityOptional.get()
            val clientEntity = stripEntity.client
            val newStripEntity = if (clientEntity?.uuid != request.clientUuid) {
                if (request.clientUuid == null) {
                    stripEntity.copy(
                        client = null,
                        name = request.name ?: stripEntity.name,
                        pin = request.pin ?: stripEntity.pin,
                        length = request.length ?: stripEntity.length,
                        height = request.height ?: stripEntity.height,
                        blendMode = request.blendMode ?: stripEntity.blendMode,
                        brightness = request.brightness ?: stripEntity.brightness,
                    )
                } else {
                    val newClientOptional = clientRepository.findByUuid(request.clientUuid)
                    if (newClientOptional.isEmpty) {
                        throw ClientRequestException("Client with uuid ${request.clientUuid} does not exist!")
                    }
                    val newClient = newClientOptional.get()

                    // Pi clients can only have one LED strip
                    if (newClient.clientType == ClientType.Pi) {
                        val newClientStrips = newClient.strips
                        if (newClientStrips.isNotEmpty() && !newClientStrips.map { it.uuid }.contains(uuid)) {
                            throw ClientRequestException("Pi clients can only have one LED strip connected.")
                        }
                    }

                    stripEntity.copy(
                        client = newClient,
                        name = request.name ?: stripEntity.name,
                        pin = request.pin ?: stripEntity.pin,
                        length = request.length ?: stripEntity.length,
                        height = request.height ?: stripEntity.height,
                        blendMode = request.blendMode ?: stripEntity.blendMode,
                        brightness = request.brightness ?: stripEntity.brightness,
                    )
                }
            } else {
                // Strip client assignment is remaining the same
                stripEntity.copy(
                    name = request.name ?: stripEntity.name,
                    pin = request.pin ?: stripEntity.pin,
                    length = request.length ?: stripEntity.length,
                    height = request.height ?: stripEntity.height,
                    blendMode = request.blendMode ?: stripEntity.blendMode,
                    brightness = request.brightness ?: stripEntity.brightness,
                )
            }

            if (request.pin != null) {
                validatePin(newStripEntity.client!!, request.pin)
            } else if (request.clientUuid != null) {
                throw ClientRequestException("A pin must be specified for a strip being assigned to a client!")
            }

            if (newStripEntity != stripEntity) {
                val newStripEntity = stripRepository.update(newStripEntity)
                val activeEffects = activeLightEffectService.getAllEffectsForStrip(uuid)
                activeEffects.forEach {
                    val newEffect = it.copy(
                        strip = SingleLedStripModel(
                            newStripEntity.name!!,
                            newStripEntity.uuid!!,
                            newStripEntity.pin!!,
                            newStripEntity.length!!,
                            newStripEntity.height,
                            newStripEntity.blendMode!!,
                            newStripEntity.brightness!!,
                            newStripEntity.client!!.uuid!!,
                            false
                        ),
                        effect = if (stripEntity.length != newStripEntity.length) recreateEffect(
                            it.effect,
                            newStripEntity.length!!
                        ) else it.effect
                    )
                    if (it != newEffect) {
                        activeLightEffectService.addOrUpdateEffect(newEffect)
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
                val effects = activeLightEffectService.getAllEffectsForStrip(uuid)
                effects.forEach {
                    activeLightEffectService.removeEffect(it)
                }
            } else {
                throw ClientRequestException("Could not delete strip with uuid $uuid. Please delete or reassign its effects and pool member strips first.")
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

    private fun isPinUnique(clientEntity: LedStripClientEntity, pin: String): Boolean {
        val occupiedPins = clientEntity.strips.map { it.pin }
        return !occupiedPins.contains(pin)
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

        val isPinUnique = isPinUnique(clientEntity, pin)
        if (!isPinUnique) {
            throw ClientRequestException("Pin $pin is already in use for this client.")
        }

        if (clientEntity.strips.map { it.pin }.contains(pin)) {
            throw ClientRequestException("Pin $pin is already used by a LED strip connected to client ${clientEntity.uuid}.")
        }
    }

    // The effect could be configured as Playing but the client is offline. If the client is offline
    // we should report 0 active effects.
    private fun getActiveEffectsForStrip(
        clientStatus: ClientStatus?, activeEffects: List<ActiveLightEffect>, stripUuid: String?
    ): Int {
        return if (clientStatus == ClientStatus.Active) activeEffects.filter { it.strip.uuid == stripUuid }.size else 0
    }

    private fun recreateEffect(lightEffect: LightEffect, numberOfLeds: Int): LightEffect {
        return when (lightEffect) {
            is BouncingBallLightEffect -> BouncingBallLightEffect(
                numberOfLeds,
                timeHelper,
                lightEffect.settings,
                lightEffect.palette
            )

            is CustomLightEffect -> CustomLightEffect(lightEffect.settings, lightEffect.palette)
            is FlameLightEffect -> FlameLightEffect(numberOfLeds, lightEffect.settings, lightEffect.palette)
            is MarqueeEffect -> MarqueeEffect(numberOfLeds, lightEffect.settings, lightEffect.palette, timeHelper)
            is NightriderLightEffect -> NightriderLightEffect(
                numberOfLeds,
                lightEffect.settings,
                lightEffect.palette,
                timeHelper
            )

            is SpectrumLightEffect -> SpectrumLightEffect(numberOfLeds, lightEffect.settings, lightEffect.palette)
            is WaveLightEffect -> WaveLightEffect(numberOfLeds, lightEffect.settings, lightEffect.palette)
            is SparkleLightEffect -> SparkleLightEffect(
                numberOfLeds,
                lightEffect.settings,
                lightEffect.palette,
                timeHelper
            )
        }
    }
}