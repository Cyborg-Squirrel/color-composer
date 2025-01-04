package io.cyborgsquirrel.util

import io.micronaut.context.annotation.Value
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Singleton
import org.h2.tools.Server
import org.slf4j.LoggerFactory

@Singleton
class H2WebServer {
    private var h2Server: Server? = null

    @Value("\${datasources.default.dialect}")
    private lateinit var dialect: Dialect

    fun start() {
        if (dialect == Dialect.H2) {
            try {
                h2Server = Server.createWebServer().start()
                logger.info("H2 webserver available at ${h2Server?.url}")
            } catch (e: Exception) {
                logger.error(e.message)
            }
        }
    }

    fun stop() {
        h2Server?.stop()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(H2WebServer::class.java)
    }
}