package io.cyborgsquirrel.util.exception

/**
 * Exception thrown by a service when a requested resource is not found
 */
class ResourceNotFoundException(message: String) : Exception(message)
