package io.cyborgsquirrel.model.client

data class LedStripClient(val uuid: String, val ipAddress: String, val websocketPort: Short, val effectIds: List<String>)