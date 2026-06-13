package io.cyborgsquirrel.messaging

/**
 * Handles a single [Query] type. [queryType] is declared explicitly for the same erasure reason as
 * `CommandHandler.commandType`.
 */
interface QueryHandler<Q : Query<R>, R> {
    val queryType: Class<Q>

    fun handle(query: Q): R
}
