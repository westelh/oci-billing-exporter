package dev.westelh.obe

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import dev.westelh.obe.client.billingBucketName
import dev.westelh.obe.client.billingNamespace
import dev.westelh.obe.client.billingPrefixForCostReport
import dev.westelh.obe.client.objectstorage.BillingObjectStorage
import dev.westelh.obe.client.objectstorage.OciBillingObjectStorage
import dev.westelh.obe.config.Config
import dev.westelh.obe.config.buildDownloadConfiguration

class CostReportDownloader(
    private val storage: BillingObjectStorage,
    private val config: Config
) {
    constructor(adp: AbstractAuthenticationDetailsProvider, config: Config) :
        this(OciBillingObjectStorage(adp, buildDownloadConfiguration(config.server.download)), config)

    companion object {
        private val logger: FluentLogger = FluentLogger.forEnclosingClass()
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
            val res = storage.listObjects(nextRequest)
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
        storage.getObject(buildRequestGetObjectOfCostReport(name))
    }.onFailure {
        logger.atWarning().withCause(it).log("Failed to download a cost report for name %s", name)
    }.getOrNull()
}
