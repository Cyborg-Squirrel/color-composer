package io.cyborgsquirrel.led_strips.enums

enum class PoolType {
    // All strips receive the same effect color data in sync
    Sync,

    // LED strips in the pool are combined and treated like one strip
    Unified
}