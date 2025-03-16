package io.cyborgsquirrel.lighting.enums

/**
 * Enum for the types of reflection
 */
enum class ReflectionType {
    /**
     * Indicates the lower half should be reflected over the upper half
     */
    LowToHigh,

    /**
     * Indicates the upper half should be reflected over the lower half
     */
    HighToLow,

    /**
     * Duplicates the lower half over the upper half and upper half over the lower half
     *
     * If a RGBColor at the copy destination is non-blank it will not be modified
     */
    CopyOverCenter,
}