package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import kotlinx.coroutines.*
import java.io.Closeable
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

class Server(private val config: Config.ServerConfig, private val client: Client) : Runnable, Closeable {
    private var httpServer: HTTPServer? = null

    private val coroutineScope: CoroutineScope = CoroutineScope(Job())
    private val updateJob: Job = coroutineScope.launch {
        try {
            while (isActive) {
                update().let { succeeded ->
                    if (succeeded) delay(config.interval)
                    else delay(config.intervalOnError)
                }
            }
        } catch (e: CancellationException) {
            logger.atSevere().withCause(e).log("Serer job is canceled because cancellation is propagated from subroutine.")
            throw e
        }
    }

    init {
        JvmMetrics.builder().register()
    }

    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    override fun run() {
        httpServer = startHTTPServer()
        updateJob.start()
    }

    suspend fun update(): Boolean {
        val all = withContext(Dispatchers.IO) {
            client.listAllCostReport().await()
        }
        if (all != null) {
            logger.atInfo().log("Downloaded %d items.", all.count())
            return true
        }
        else {
            logger.atWarning().log("Download failed. resulted content: ", all)
            return false
        }
    }

    override fun close() {
        logger.atInfo().log("Server is shutting down gracefully...")
        httpServer?.close()
        coroutineScope.cancel("Server is closed by close()")
    }

    private fun startHTTPServer(): HTTPServer {
        with(config) {
            if (hostname.isNotBlank()) return startHttpServerOnHostname(port, hostname)
            if (inetAddress.isNotBlank()) {
                try {
                    val inet = InetAddress.getByName(inetAddress)
                    return startHttpServerOnInet(port, inet)
                } catch (e: UnknownHostException) {
                    logger.atSevere().withCause(e)
                        .log("inetAddress %s is not a valid address. Server cannot bind to it.", inetAddress)
                    throw e
                }
            }

            logger.atWarning().log("Both inetAddress and hostname is specified. Binding server to localhost.")
            return startHttpServerOnInet(port, InetAddress.getLocalHost())
        }
    }

    suspend fun join() = updateJob.join()
}

// HTTPServer.Builder take inetAddress or hostname. Cannot take both.
private fun startHttpServerOnInet(port: Int, inetAddress: InetAddress) = HTTPServer.builder()
    .port(port)
    .inetAddress(inetAddress)
    .buildAndStart()
private fun startHttpServerOnHostname(port: Int, hostname: String) = HTTPServer.builder()
    .port(port)
    .hostname(hostname)
    .buildAndStart()
