package io.cyborgsquirrel.lighting.rendering.cache

import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets

/**
 * Provides a reusable per-strip [RgbColor] buffer so the render path can copy effect output and run post-processing
 * without allocating a fresh array every frame. The buffer holds stable, distinct [RgbColor] instances; callers
 * mutate the values in place. A new array is only allocated when a strip is first seen or its length changes.
 */
class StripFrameCache {

    private val cache = mutableMapOf<String, Array<RgbColor>>()

    fun getOrCreate(stripUuid: String, length: Int): Array<RgbColor> {
        val existing = cache[stripUuid]
        return if (existing != null && existing.size == length) {
            existing
        } else {
            Array(length) { RgbColorPresets.blank() }.also { cache[stripUuid] = it }
        }
    }
}
