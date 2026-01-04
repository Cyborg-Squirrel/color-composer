package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect

interface ActiveLightEffectChangeListener {
    fun onUpdate(newEffects: List<ActiveLightEffect>)
}