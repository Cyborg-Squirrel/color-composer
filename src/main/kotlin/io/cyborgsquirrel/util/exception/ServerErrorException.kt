package io.cyborgsquirrel.util.exception

/**
 * Exception thrown if the api encounters an error processing a valid client request
 */
class ServerErrorException(message: String) : Exception(message)