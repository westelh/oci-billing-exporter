package dev.westelh.obe

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.google.common.flogger.FluentLogger
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import dev.westelh.obe.client.billingBucketName
import dev.westelh.obe.client.billingNamespace
import dev.westelh.obe.client.objectstorage.CachedObjectStorage
import dev.westelh.obe.client.objectstorage.SimpleRequestFactory
import dev.westelh.obe.client.objectstorage.listAllObjects
import dev.westelh.obe.client.objectstorage.suspendGetObject
import dev.westelh.obe.config.Config
import dev.westelh.obe.config.buildDownloadConfiguration
import dev.westelh.obe.config.loadAuthConfig
import dev.westelh.obe.core.CsvParser
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.zip.GZIPInputStream
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

class Run : CliktCommand() {
    private val configFile by option("--config").file(mustBeReadable = true, canBeDir = false).required()
    private val metrics = Metrics()

    init {
        logger.atFine().log("Server is starting...")
        JvmMetrics.builder().register()
    }

    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    override fun run() {
        val config = Config.fromYaml(configFile)
        val adp = loadAuthConfig(config.auth).getOrThrow()
        val factory = CachedObjectStorage(adp)
        val os = factory.getObjectStorage()
        val osAsync = factory.getObjectStorageAsync()
        val dlManager = DownloadManager(os, buildDownloadConfiguration(config.server.download))

        runBlocking {
            val requestFactory = SimpleRequestFactory(config.targetTenantId)

            logger.atFine().log("Starting main loop.")
            loop(config.server.delay.toKotlinDuration()) {
                try {
                    val all = listAllObjects(osAsync, requestFactory.buildListCostReportsRequest())
                    if (all.isNotEmpty()) {
                        val latest = all.maxBy { it.name }.name
                        val getRes = dlManager.suspendGetObject(
                            GetObjectRequest.builder().billingNamespace().billingBucketName(config.targetTenantId)
                                .objectName(latest).build()
                        )
                        val report = CsvParser().parse(GZIPInputStream(getRes.inputStream))
                        report.items.forEach { metrics.record(it) }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    logger.atWarning().withCause(e).log("Updating metrics is cancelled because: %s", e.message)
                }
                logger.atInfo().log("Update is finished. Sleeping for %s.", config.server.delay)
            }
        }
    }
}

suspend fun<R> loop(delay: Duration, job: suspend () -> R) {
    while (true) {
        job()
        delay(delay)
    }
}