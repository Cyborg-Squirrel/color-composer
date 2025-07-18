package io.cyborgsquirrel.clients.config.pi_client

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class PiClientConfigList(val configList: List<PiClientConfig>)