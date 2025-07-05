package io.cyborgsquirrel.util

fun Int.toLittleEndian(byteCount: Int): ByteArray {
    // Shift right by 8 (8 bits per byte)
    // Logical and with FF to capture the eight lowest bits
    return ByteArray(byteCount) { i ->
        ((this shr (8 * i)) and 0xFF).toByte()
    }
}

fun Long.toLittleEndian(byteCount: Int): ByteArray {
    // Shift right by 8 (8 bits per byte)
    // Logical and with FF to capture the eight lowest bits
    return ByteArray(byteCount) { i ->
        ((this shr (8 * i)) and 0xFF).toByte()
    }
}