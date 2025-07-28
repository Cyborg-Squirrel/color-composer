package io.cyborgsquirrel.clients.enums

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class ColorOrder {
    RGB,
    RBG,
    GRB,
    GBR,
    BRG,
    BGR,
}