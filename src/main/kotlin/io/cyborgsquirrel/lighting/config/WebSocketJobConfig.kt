package io.cyborgsquirrel.lighting.config

data class WebSocketJobConfig(val ipAddress: String, val port: Int, val lightStripIds: List<String>)