package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.model.color.RgbColor

/**
 * Class for user generated custom lighting effects
 */
class CustomLightEffect(uuid: String, name: String, numberOfLeds: Int) : LightEffect(uuid, name, numberOfLeds) {

    override fun getNextStep(): List<RgbColor> {
        TODO("Not yet implemented")
    }

    override fun getIterations(): Int {
        TODO("Not yet implemented")
    }
}