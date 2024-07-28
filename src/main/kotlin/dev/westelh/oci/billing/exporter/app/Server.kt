package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import dev.westelh.oci.billing.exporter.core.CsvParser
import dev.westelh.oci.billing.exporter.core.Metrics
import dev.westelh.oci.billing.exporter.core.record
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import kotlinx.coroutines.*
import java.io.Closeable
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.zip.GZIPInputStream
import kotlin.coroutines.cancellation.CancellationException

class Server(private val config: Config.ServerConfig, private val client: Client) : Runnable, Closeable {
    private var httpServer: HTTPServer? = null
    private val metrics: Metrics = Metrics()
    private val coroutineScope: CoroutineScope = CoroutineScope(Job())
    private val updateJob: Job = coroutineScope.launch {
        try {
            while (isActive) {
                withContext(Dispatchers.IO) {
                    // Enumerate all items
                    val all = client.listAllCostReport().orEmpty()
                    logger.atFine().log("Update Job: listing all cost report: got %d items", all.count())

                    // Pick latest
                    val latest = if (all.isNotEmpty()) {
                        all.maxBy { it.name }   // pick latest by its name
                    } else {
                        return@withContext  // fast-return when list is empty
                    }
                    logger.atFine().log("Update Job: selected latest cost report: %s", latest)

                    // Download
                    val download = client.downloadByName(latest.name) ?: return@withContext // fast-return when download failed
                    logger.atFine().log("Update Job: downloaded cost report object")

                    // Parse
                    val parser = CsvParser()
                    val report = parser.parse(GZIPInputStream(download.inputStream)) ?: return@withContext   // fast-return when parse failed
                    logger.atFine().log("Update Job: parsed cost report csv")

                    // Write metrics
                    for (item in report.items) metrics.record(item)
                    logger.atFine().log("Update Job: wrote %d items in metrics", report.items.count())

                    logger.atInfo().log("Update job finished")
                }
                logger.atInfo().log("Suspending update job for %d milliseconds", config.interval)
                delay(config.interval)
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
