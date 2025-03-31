package io.cyborgsquirrel.util.exception

/**
 * Exception thrown by a service when a user's request is invalid
 */
class ClientRequestException(message: String) : Exception(message)