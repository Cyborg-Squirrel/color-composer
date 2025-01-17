package io.cyborgsquirrel.model.strip

interface LedStrip {
    fun getName(): String

    fun getUuid(): String

    fun getLength(): Int
}