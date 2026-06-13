package io.cyborgsquirrel.messaging

/**
 * Result of a command that creates or resolves a resource, carrying its identifier.
 * [uuid] - the uuid of the resource being commanded
 */
data class CommandResponse(val uuid: String)

