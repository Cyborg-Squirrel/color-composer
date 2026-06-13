package io.cyborgsquirrel.messaging

import jakarta.inject.Singleton

/**
 * Query-side counterpart to `CommandBusImpl`: reflection-free `Class<query> -> handler` routing.
 */
@Singleton
class QueryBusImpl(
    handlers: List<QueryHandler<*, *>>,
) : QueryBus {

    private val handlersByType: Map<Class<*>, QueryHandler<*, *>> =
        handlers.associateBy { it.queryType }

    @Suppress("UNCHECKED_CAST")
    override fun <R> send(query: Query<R>): R {
        val handler = handlersByType[query::class.java] as? QueryHandler<Query<R>, R>
            ?: throw IllegalStateException(
                "No query handler registered for ${query::class.java.name}"
            )
        return handler.handle(query)
    }
}
