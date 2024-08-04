package dev.westelh.obe.client

import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest

class SimpleRequestFactory(targetTenantId: String) : RequestFactory {
    private val namespace: String = "bling"
    private val costReportPrefix: String = "reports/cost-csv"
    private val usageReportPrefix: String = "reports/usage-csv"
    private val bucketName: String = targetTenantId

    override fun buildListCostReportsRequest(start: String): ListObjectsRequest {
        return ListObjectsRequest.builder()
            .namespaceName(namespace)
            .bucketName(bucketName)
            .prefix(costReportPrefix)
            .start(start)
            .build()
    }

    override fun buildListUsageReportsRequest(): ListObjectsRequest {
        return ListObjectsRequest.builder()
            .namespaceName(namespace)
            .bucketName(bucketName)
            .prefix(usageReportPrefix)
            .build()
    }

    override fun buildGetReportRequest(objectName: String): GetObjectRequest {
        return GetObjectRequest.builder()
            .namespaceName(namespace)
            .bucketName(bucketName)
            .objectName(objectName)
            .build()
    }
}