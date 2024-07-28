package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import com.oracle.bmc.responses.AsyncHandler
import dev.westelh.oci.billing.exporter.client.ObjectStorageFactory
import dev.westelh.oci.billing.exporter.client.OnDemandObjectStorage
import dev.westelh.oci.billing.exporter.client.await

class Client(config: Config) {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private val adp = loadAuthConfig(config.auth).getOrThrow()
    private val requestFactory: RequestFactory = SimpleRequestFactory(config.targetTenantId)
    private val osFactory: ObjectStorageFactory = OnDemandObjectStorage(adp)
    private val dlManagerConfig = buildDownloadConfiguration(config.server.download)

    suspend fun listAllCostReport(): List<ObjectSummary>? {
        val ret = mutableListOf<ObjectSummary>()
        var nextStartWith = ""
        // Enumeration
        do {
            val request = requestFactory.buildListCostReportsRequest(nextStartWith)
            val response = kotlin.runCatching {
                osFactory.getObjectStorageAsync().listObjects(request, CustomAsyncHandler()).await()
            }.getOrElse { return null /* Fast-return on error */ }
            ret.addAll(response.listObjects.objects)
            nextStartWith = response.listObjects.nextStartWith ?: ""
        } while (nextStartWith.isNotBlank())
        return ret
    }

    // suspend fun listUsageReport():

    fun downloadByName(objectName: String): GetObjectResponse? {
        logger.atInfo().log("Started downloading %s", objectName)

        val dlManager = DownloadManager(osFactory.getObjectStorage(), dlManagerConfig)

        val request = requestFactory.buildGetReportRequest(objectName)
        logger.atFinest().log("Prepared request for downloading object by name: %s", request)

        // Downloading may fail
        try {
            val response = dlManager.getObject(request)
            logger.atFine().log("Downloaded %d bytes", response.contentLength)
            return response
        } catch (e: BmcException) {
            logger.logWithBmcException(e)
            return null
        }
    }

    class CustomAsyncHandler<Req, Res> : AsyncHandler<Req, Res> {
        override fun onSuccess(req: Req, res: Res) {
            logger.atFine().log("Client completed an async call, that is: %s", req)
        }

        override fun onError(req: Req, t: Throwable?) {
            logger.atWarning().withCause(t).log("Client failed invoking async call to: %s", req)
        }
    }
}
