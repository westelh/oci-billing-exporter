package dev.westelh.obe

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import com.oracle.bmc.responses.AsyncHandler
import dev.westelh.obe.client.*
import dev.westelh.obe.client.objectstorage.ObjectStorageFactory
import dev.westelh.obe.client.objectstorage.OnDemandObjectStorage
import dev.westelh.obe.client.objectstorage.RequestFactory
import dev.westelh.obe.client.objectstorage.SimpleRequestFactory
import dev.westelh.obe.config.Config
import dev.westelh.obe.config.buildDownloadConfiguration
import dev.westelh.obe.config.loadAuthConfig
import dev.westelh.obe.core.CostReport
import dev.westelh.obe.core.CsvParser
import java.util.zip.GZIPInputStream

class Client(config: Config) {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private val adp = loadAuthConfig(config.auth).getOrThrow()
    private val requestFactory: RequestFactory = SimpleRequestFactory(config.targetTenantId)
    private val osFactory: ObjectStorageFactory = OnDemandObjectStorage(adp)
    private val dlManagerConfig = buildDownloadConfiguration(config.server.download)

    suspend fun listAllCostReport(): List<ObjectSummary>? {
        val summaries = mutableListOf<ObjectSummary>()
        var nextStartWith = ""

        do {
            val request = requestFactory.buildListCostReportsRequest(nextStartWith)
            val response = kotlin.runCatching {
                osFactory.getObjectStorageAsync().listObjects(request, CustomAsyncHandler()).await()
            }.getOrElse { return null /* Fast-return on error */ }
            summaries.addAll(response.listObjects.objects)
            nextStartWith = response.listObjects.nextStartWith ?: ""
        } while (nextStartWith.isNotBlank())
        return summaries
    }

    // suspend fun listUsageReport():

    fun downloadCostReport(objectName: String): CostReport? {
        val dlManager = DownloadManager(osFactory.getObjectStorage(), dlManagerConfig)
        val request = requestFactory.buildGetReportRequest(objectName)
        logger.atFinest().log("Prepared a request for downloading cost report by name: %s", request)

        return kotlin.runCatching {
            dlManager.getObject(request).inputStream.use { CsvParser().parse(GZIPInputStream(it)) }
        }.getOrElse {
            logger.atWarning().withCause(it).log("Downloading has failed with an exception")
            null
        }
    }

    suspend fun downloadLatestCostReport(): CostReport? {
        val latest = listAllCostReport().orEmpty().latest() ?: return null
        return downloadCostReport(latest.name)
    }

    // fun downloadUsageReport()

    class CustomAsyncHandler<Req, Res> : AsyncHandler<Req, Res> {
        override fun onSuccess(req: Req, res: Res) {
            logger.atFine().log("Client completed an async call, that is: %s", req)
        }

        override fun onError(req: Req, t: Throwable?) {
            logger.atWarning().withCause(t).log("Client failed invoking async call to: %s", req)
        }
    }
}

private fun List<ObjectSummary>.latest(): ObjectSummary? = maxByOrNull { it.name }