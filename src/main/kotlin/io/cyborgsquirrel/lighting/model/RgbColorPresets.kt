package io.cyborgsquirrel.lighting.model

class RgbColorPresets {
    companion object {
        fun red() = RgbColor(255u, 0u, 0u)
        fun orange() = RgbColor(255u, 50u, 0u)
        fun amber() = RgbColor(255u, 160u, 0u)
        fun yellow() = RgbColor(255u, 255u, 0u)
        fun green() = RgbColor(0u, 255u, 0u)
        fun cyan() = RgbColor(0u, 255u, 255u)
        fun blue() = RgbColor(0u, 0u, 255u)
        fun violet() = RgbColor(148u, 0u, 211u)
        fun purple() = RgbColor(128u, 0u, 255u)
        fun pink() = RgbColor(255u, 0u, 150u)
        fun white() = RgbColor(255u, 255u, 255u)
        fun blank() = RgbColor(0u, 0u, 0u)
        fun rainbow() = listOf(red(), orange(), yellow(), green(), blue(), purple())
    }
}