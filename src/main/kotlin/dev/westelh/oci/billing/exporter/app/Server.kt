package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.objectstorage.model.ObjectSummary
import dev.westelh.oci.billing.exporter.core.*
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import java.io.InputStream

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
            sequence()
        }
    }

    fun sequence() {
        val loaded = downloadNewestReport()
        val parsed = loaded?.let { parse(it) }
        parsed?.let { updateMetrics(it) }
    }

    fun downloadNewestReport(): InputStream? = try {
        serviceController.whenServiceAvailable {
            val allReports = listAllCostReports(tenancy)
            val newest = allReports.newest()
            downloadObjectByName(newest.name)
        }
    } catch(e: Exception) {
        logger.atWarning().log("Failed to download newest cost report with an exception: %s", e)
        null
    }

    fun parse(downloadStream: InputStream): BillingReport? = try {
        CsvParser().parse(downloadStream)
    } catch (e: Exception) {
        logger.atWarning().log("Failed to parse a report loaded from the server with an exception: %s", e)
        null
    }

    fun updateMetrics(newReport: BillingReport) = try {
        for (i in newReport.items) {
            metrics.record(i)
        }
    } catch (e: Exception) {
        logger.atWarning().log("Failed to write a metrics with an exception: %s", e)
    }
}

fun MutableIterable<ObjectSummary>.newest(): ObjectSummary = maxBy { it.name }
