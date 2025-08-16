package io.cyborgsquirrel.clients.status

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import java.util.*

interface ClientStatusService {
    fun getStatusForClient(clientEntity: LedStripClientEntity): Optional<ClientStatusInfo>
}