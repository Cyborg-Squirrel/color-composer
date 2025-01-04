package io.cyborgsquirrel.job.config

data class WebSocketJobConfig(val ipAddress: String, val port: Int, val lightStripIds: List<String>)