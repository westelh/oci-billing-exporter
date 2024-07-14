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

    private val serviceController = ServiceController(auth, tenancy)
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
        }
    }

    private fun updateMetrics() {
        serviceController.whenServiceAvailable {    // If client service is available
            val allReports = listAllCostReports(tenancy)
            val newest = allReports.newest()
            val newestReport = downloadObjectByName(newest.name)
            val items = CsvParser().parse(GZIPInputStream(newestReport.inputStream)).items
            for (i in items) {
                metrics.record(i)
            }
            logger.atInfo().log("Updated %s items", items.size)
        }
    }
}

fun MutableIterable<ObjectSummary>.newest(): ObjectSummary = maxBy { it.name }
