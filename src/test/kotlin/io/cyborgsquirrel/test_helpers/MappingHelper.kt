package io.cyborgsquirrel.test_helpers

fun normalizeNumberTypes(value: Any): Any {
    return when (value) {
        is Double -> value.toFloat()
        else -> value
    }
}