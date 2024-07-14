package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.objectstorage.model.ObjectSummary
import dev.westelh.oci.billing.exporter.core.CsvParser
import dev.westelh.oci.billing.exporter.core.Metrics
import dev.westelh.oci.billing.exporter.client.RequestFactory
import dev.westelh.oci.billing.exporter.core.record
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import java.util.zip.GZIPInputStream

class Server(private val options: App.ServerOptions, private val tenancy: String, private val auth: AuthArguments) : Runnable {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private val serviceController = ServiceController(auth)
    private val requestFactory = RequestFactory(tenancy)
    private val metrics = Metrics()

    private val interval = options.interval
    private val intervalOnError = 10000L

    override fun run() {
        JvmMetrics.builder().register()
        val server: HTTPServer = HTTPServer.builder()
            .port(options.port)
            .buildAndStart()
        logger.atInfo().log("Server in running on port %s", server.port)

        while (true) {
            updateMetrics()
                .onSuccess {
                    logger.atInfo().log("Server suspends until next update for %d milliseconds", interval)
                    Thread.sleep(interval)
                }
                .onFailure {
                    logger.atSevere().log("Failed to update metrics: %s", it)
                    logger.atInfo().log("Server suspends until next try for %d milliseconds", intervalOnError)
                    Thread.sleep(intervalOnError)
                }
        }
    }

    private fun updateMetrics(): Result<Unit> {
        serviceController.getService()?.run {    // If client service is available
            val allReports = listAllCostReports(tenancy).onFailure { cause ->
                return Result.failure(
                    RuntimeException(
                        "Failed listing object storage bucket containing cost reports",
                        cause
                    )
                )
            }.getOrThrow()
            val newestReport = allReports.newest()
            val report =
                downloadObject(requestFactory.createGetCostReportRequest(newestReport.name)).onFailure { cause ->
                    return Result.failure(RuntimeException("Failed downloading cost report from object storage", cause))
                }.getOrThrow()
            val items = CsvParser().parse(GZIPInputStream(report.inputStream)).items
            for (i in items) {
                metrics.record(i)
            }
            logger.atInfo().log("Updated %s items", items.size)
            return Result.success(Unit)
        }
        return Result.failure(RuntimeException("Skipped updating metrics because client is not ready."))
    }

    private fun Iterable<ObjectSummary>.newest() = maxBy { it.name }
}


