package io.cyborgsquirrel.clients.query

import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.messaging.Query

/** Fetches a single client by uuid. */
data class GetClientQuery(val uuid: String) : Query<GetClientResponse>
