package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.objectstorage.model.ObjectSummary
import dev.westelh.oci.billing.exporter.core.*
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import kotlinx.coroutines.*
import java.io.InputStream
import java.lang.Runnable
import kotlin.coroutines.coroutineContext

class Server(private val options: App.ServerOptions, private val tenancy: String, private val auth: AuthArguments) : Runnable {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private val serviceController = ServiceController(auth, tenancy)
    private val metrics = Metrics()
    private var loopHandler: Job? = null

    override fun run() {
        JvmMetrics.builder().register()
        val server: HTTPServer = HTTPServer.builder()
            .port(options.port)
            .buildAndStart()
        logger.atInfo().log("Server in running on port %s", server.port)

        runBlocking {
           loopHandler = launch {
               try {
                   while (true) {
                       val result = sequence()
                       if (result) delay(options.interval)
                       else delay(options.intervalOnError)
                   }
               } finally {
                   logger.atInfo().log("Server quit")
               }
           }
        }
    }

    fun sequence(): Boolean {
        val loaded = downloadNewestReport()
        val parsed = loaded?.let { parse(it) }
        parsed?.let {
            updateMetrics(it)
            return true
        }
        return false
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

    fun shutdown() {
        runBlocking {
            loopHandler?.cancelAndJoin()
        }
    }
}

fun MutableIterable<ObjectSummary>.newest(): ObjectSummary = maxBy { it.name }
