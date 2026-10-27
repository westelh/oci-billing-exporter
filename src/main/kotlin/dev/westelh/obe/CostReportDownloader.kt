package dev.westelh.obe

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import dev.westelh.obe.client.billingBucketName
import dev.westelh.obe.client.billingNamespace
import dev.westelh.obe.client.billingPrefixForCostReport
import dev.westelh.obe.client.objectstorage.suspendGetObject
import dev.westelh.obe.client.objectstorage.suspendListObjects
import dev.westelh.obe.config.Config
import dev.westelh.obe.config.buildDownloadConfiguration

class CostReportDownloader private constructor(
    private val config: Config,
    private val operations: Operations
) {
    constructor(adp: AbstractAuthenticationDetailsProvider, config: Config) : this(config, sdkOperations(adp, config))

    internal constructor(
        config: Config,
        listObjects: suspend (ListObjectsRequest) -> ListObjectsResponse,
        getObject: suspend (GetObjectRequest) -> GetObjectResponse
    ) : this(config, Operations(listObjects, getObject))

    internal class Operations(
        val listObjects: suspend (ListObjectsRequest) -> ListObjectsResponse,
        val getObject: suspend (GetObjectRequest) -> GetObjectResponse
    )

    companion object {
        private val logger: FluentLogger = FluentLogger.forEnclosingClass()

        private fun sdkOperations(adp: AbstractAuthenticationDetailsProvider, config: Config): Operations {
            val objectStorage = ObjectStorageClient.builder().build(adp)
            val objectStorageAsync = ObjectStorageAsyncClient.builder().build(adp)
            val downloadManager = DownloadManager(objectStorage, buildDownloadConfiguration(config.server.download))
            return Operations(
                listObjects = { objectStorageAsync.suspendListObjects(it) },
                getObject = { downloadManager.suspendGetObject(it) }
            )
        }
    }

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

    suspend fun listAllCostReport(): List<ObjectSummary>? = runCatching {
        val sum = mutableListOf<ObjectSummary>()
        var nextStartWith = ""

        do {
            val nextRequest = buildRequestListObjectOfCostReport(nextStartWith)
            val res = operations.listObjects(nextRequest)
            sum.addAll(res.listObjects.objects)
            nextStartWith = res.listObjects.nextStartWith ?: ""
        } while (nextStartWith.isNotBlank())

        return sum
    }.onSuccess {
        logger.atInfo().log("Retrieved all %d summaries of report object")
    }.onFailure {
        logger.atWarning().withCause(it).log("Pagination has stopped due to an error")
    }.getOrNull()

    suspend fun downloadCostReport(name: String): GetObjectResponse? = runCatching {
        operations.getObject(buildRequestGetObjectOfCostReport(name))
    }.onFailure {
        logger.atWarning().withCause(it).log("Failed to download a cost report for name %s", name)
    }.getOrNull()
}
