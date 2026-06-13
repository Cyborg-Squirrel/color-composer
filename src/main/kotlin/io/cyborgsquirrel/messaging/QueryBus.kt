package io.cyborgsquirrel.messaging

/**
 * Resolves the [QueryHandler] for a given [Query] and invokes it through the middleware pipeline.
 * Controllers depend only on this, never on concrete handlers.
 */
interface QueryBus {
    fun <R> send(query: Query<R>): R
}
