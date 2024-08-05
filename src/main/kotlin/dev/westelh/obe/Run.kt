package dev.westelh.obe

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import dev.westelh.obe.client.billingBucketName
import dev.westelh.obe.client.billingNamespace
import dev.westelh.obe.client.billingPrefixForCostReport
import dev.westelh.obe.client.objectstorage.listAllObjects
import dev.westelh.obe.client.objectstorage.suspendGetObject
import dev.westelh.obe.config.*
import dev.westelh.obe.core.CsvParser
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.zip.GZIPInputStream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toKotlinDuration

class Run : CliktCommand() {
    private val config by option("--config").file(mustBeReadable = true, canBeDir = false).convert {
        Config.fromYaml(it)
    }.required()

    private val metrics = Metrics()

    init {
        JvmMetrics.builder().register()
    }

    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    override fun run() {
        runBlocking {

            logger.atInfo().log("Starting main loop.")
            loop(config.server.delay.toKotlinDuration()) {
                try {

                    // To get SDK initialized eagerly, trying build them inside loop and try{}.
                    val auth = getAuthentication()
                    val objectStorage = buildObjectStorage(auth)
                    val objectStorageAsync = buildObjectStorageAsync(auth)
                    val downloadManager = buildDownloadManager(objectStorage)

                    logger.atFine().log("Getting the list of objects in the designated object storage bucket.")
                    val all = listAllObjects(objectStorageAsync, buildRequestListObjectOfCostReport())

                    if (all.isNotEmpty()) {
                        val latest = all.maxBy { it.name }.name

                        logger.atFine().log("Downloading the latest cost report.")
                        val getRes = downloadManager.suspendGetObject(buildRequestGetObjectOfCostReport(latest))

                        logger.atFine().log("Parsing object content as cost report.")
                        val report = CsvParser().parse(GZIPInputStream(getRes.inputStream))

                        logger.atFine().log("Writing metrics for each items in the cost report.")
                        report.items.forEach { metrics.record(it) }

                        logger.atInfo().log("Downloaded the latest cost report, created at %s, with %d items.", 0, report.items.count())
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    logger.atWarning().withCause(e).log("Updating metrics is cancelled because: %s", e.message)
                }
                logger.atInfo().log("Update is finished. Sleeping for %s.", config.server.delay.toKotlinDuration().toString(DurationUnit.SECONDS))
            }
        }
    }

    private fun getAuthentication(): AuthenticationDetailsProvider =
        config.auth.anythingAvailable() ?: throw RuntimeException("All of the auth methods provided was unavailable.")

    private fun buildObjectStorage(adp: AuthenticationDetailsProvider): ObjectStorage = ObjectStorageClient.builder().build(adp)

    private fun buildObjectStorageAsync(adp: AuthenticationDetailsProvider): ObjectStorageAsync = ObjectStorageAsyncClient.builder().build(adp)

    private fun buildDownloadManager(objectStorage: ObjectStorage): DownloadManager =
        DownloadManager(objectStorage, buildDownloadConfiguration(config.server.download))

    private fun buildRequestListObjectOfCostReport(): ListObjectsRequest = ListObjectsRequest.builder()
        .billingNamespace()
        .billingBucketName(config.targetTenantId)
        .billingPrefixForCostReport()
        .build()

    private fun buildRequestGetObjectOfCostReport(name: String): GetObjectRequest = GetObjectRequest.builder()
        .billingNamespace()
        .billingBucketName(config.targetTenantId)
        .objectName(name)
        .build()
}

suspend fun <R> loop(delay: Duration, job: suspend () -> R) {
    while (true) {
        job()
        delay(delay)
    }
}