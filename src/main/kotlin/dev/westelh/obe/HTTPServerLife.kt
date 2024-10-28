package dev.westelh.obe

import com.google.common.flogger.FluentLogger
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import java.io.Closeable

class HTTPServerLife(builder: HTTPServer.Builder) : Closeable {
    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    init {
        logger.atInfo().log("Starting HTTP server...")
    }

    private val httpServer: HTTPServer = try {
        builder.buildAndStart()
    } catch (e: Exception) {
        logger.atSevere().withCause(e).log("Could not start HTTP server with given configuration.")
        throw e
    }

    init {
        logger.atInfo().log("Started HTTP server on port %s", httpServer.port)
    }

    override fun close() {
        logger.atInfo().log("Stopping HTTP server...")
        httpServer.close()
    }
}

