package io.cyborgsquirrel.clients.config

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class PiClientConfigList(val configList: List<PiClientConfig>)