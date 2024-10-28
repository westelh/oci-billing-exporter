package dev.westelh.obe

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import dev.westelh.obe.client.billingBucketName
import dev.westelh.obe.client.billingNamespace
import dev.westelh.obe.client.billingPrefixForCostReport
import dev.westelh.obe.client.objectstorage.suspendGetObject
import dev.westelh.obe.client.objectstorage.suspendListObjects
import dev.westelh.obe.config.*
import dev.westelh.obe.core.JacksonCsvParser
import io.prometheus.metrics.exporter.httpserver.HTTPServer
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

    // TODO: This code should run in run()
    init {
        JvmMetrics.builder().register()
    }

    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    override fun run() {
        val builder = HTTPServer.builder()
            .port(config.server.port)
            .inetAddress(config.server.host)

        val httpServerLife = HTTPServerLife(builder)
        addShutdownHook(httpServerLife)

        httpServerLife.use {
            runBlocking {

                logger.atInfo().log("Starting main loop.")
                loop(config.server.delay.toKotlinDuration()) {
                    try {

                        // To get SDK initialized eagerly, trying build them inside loop and try{}.
                        val auth = getAuthentication()
                        val objectStorage = buildObjectStorage(auth)
                        val objectStorageAsync = buildObjectStorageAsync(auth)
                        val downloadManager = buildDownloadManager(objectStorage)

                        val all = listAllCostReport(objectStorageAsync)
                        logger.atInfo().log("Retrieved a summary of %d cost reports.", all.count())

                        if (all.isNotEmpty()) {
                            val latest = all.maxBy { it.name }.name

                            logger.atFine().log("Downloading the latest cost report.")
                            val getRes = downloadManager.suspendGetObject(buildRequestGetObjectOfCostReport(latest))

                            logger.atFine().log("Parsing object content as cost report.")
                            val report = getRes.inputStream.use { JacksonCsvParser().parse(GZIPInputStream(it)) }

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
    }

    private fun getAuthentication(): AbstractAuthenticationDetailsProvider {
        return with(config.auth) {
            runCatching {
                loadFileConfig(config)
            }.recoverCatching {
                logger.atFiner().withCause(it).log("File config is invalid.")
                logger.atFine().log("Recovering authentication by instance principals.")
                loadInstancePrincipalConfig(
                    instancePrincipal ?: Config.AuthConfig.InstancePrincipalConfig()
                )
            }.recoverCatching {
                logger.atFiner().withCause(it).log("Instance principal is not available.")
                logger.atFine().log("Recovering authentication by resource principals.")
                loadResourcePrincipalConfig(
                    resourcePrincipal ?: Config.AuthConfig.ResourcePrincipalConfig()
                )
            }.getOrElse {
                logger.atFiner().withCause(it).log("Resource principal is not available.")
                throw RuntimeException("All of authentication method failed.")
            }
        }
    }

    private fun buildObjectStorage(adp: AbstractAuthenticationDetailsProvider): ObjectStorage = ObjectStorageClient.builder().build(adp)

    private fun buildObjectStorageAsync(adp: AbstractAuthenticationDetailsProvider): ObjectStorageAsync = ObjectStorageAsyncClient.builder().build(adp)

    private fun buildDownloadManager(objectStorage: ObjectStorage): DownloadManager =
        DownloadManager(objectStorage, buildDownloadConfiguration(config.server.download))

    private fun buildRequestListObjectOfCostReport(start: String = ""): ListObjectsRequest = ListObjectsRequest.builder()
        .billingNamespace()
        .billingBucketName(config.targetTenantId)
        .billingPrefixForCostReport()
        .start(start)
        .build()

    private fun buildRequestGetObjectOfCostReport(name: String): GetObjectRequest = GetObjectRequest.builder()
        .billingNamespace()
        .billingBucketName(config.targetTenantId)
        .objectName(name)
        .build()

    private suspend fun listAllCostReport(objectStorageAsync: ObjectStorageAsync): List<ObjectSummary> {
        logger.atFine().log("Started getting the list of objects in the designated object storage bucket.")

        val sum = mutableListOf<ObjectSummary>()
        var nextStartWith = ""

        do {
            logger.atFiner().log("Preparing a request for a list of objects that start with %s", nextStartWith)
            val nextRequest = buildRequestListObjectOfCostReport(nextStartWith)

            logger.atFiner().log("Suspend the coroutine until the request completes: %s", nextRequest)
            val res = objectStorageAsync.suspendListObjects(nextRequest)

            sum.addAll(res.listObjects.objects)
            nextStartWith = res.listObjects.nextStartWith ?: ""
        } while (nextStartWith.isNotBlank())

        return sum
    }

    private fun addShutdownHook(httpServerLife: HTTPServerLife) {
        Runtime.getRuntime().addShutdownHook(Thread {
            httpServerLife.close()
        })
    }
}

suspend fun <R> loop(delay: Duration, job: suspend () -> R) {
    while (true) {
        job()
        delay(delay)
    }
}