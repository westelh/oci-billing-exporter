package dev.westelh.oci.billing.exporter.core

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.transfer.DownloadConfiguration
import dev.westelh.oci.billing.exporter.api.*
import dev.westelh.oci.billing.exporter.app.SubCommand
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import java.util.zip.GZIPInputStream

class Server(private val options: SubCommand.ServerOptions, tenancy: String, client: ObjectStorageClient): Runnable {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private val service = ConcreteClientService(client)
    private val downloadManager = ObjectDownloadManager(client, DownloadConfiguration.builder().build())
    private val requestFactory = RequestFactory(tenancy)
    private val metrics = Metrics()

    override fun run() {
        JvmMetrics.builder().register()
        val server: HTTPServer = HTTPServer.builder()
            .port(options.port)
            .buildAndStart()
        logger.atInfo().log("Server in running on port %s", server.port)

        while (true) {
            updateMetrics()
                .onSuccess {
                    logger.atInfo().log("Server suspends until next update after %d milliseconds", options.interval)
                    Thread.sleep(options.interval)
                }
                .onFailure {
                    logger.atSevere().log("Failed to update metrics: %s", it)
                    Thread.sleep(10000)
                }
        }
    }

    private fun updateMetrics(): Result<Unit> {
        val allReports = service.iterateObjects(requestFactory.createListCostReportsRequest()).onFailure { cause ->
            return Result.failure(RuntimeException("Failed listing object storage bucket containing cost reports", cause))
        }.getOrThrow()
        val newestReport = allReports.newest()
        val report = downloadManager.download(requestFactory.createGetCostReportRequest(newestReport.name)).onFailure { cause ->
            return Result.failure(RuntimeException("Failed downloading cost report from object storage", cause))
        }.getOrThrow()
        val items = CsvParser().parse(GZIPInputStream(report.inputStream)).items
        for (i in items) {
            metrics.record(i)
        }
        logger.atInfo().log("Updated %s items", items.size)
        return Result.success(Unit)
    }

    private fun Iterable<ObjectSummary>.newest() = maxBy { it.name }
}


