package dev.westelh.obe.client.objectstorage

import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import dev.westelh.obe.client.billingBucketName
import dev.westelh.obe.client.billingNamespace
import dev.westelh.obe.client.billingPrefixForCostReport
import dev.westelh.obe.client.billingPrefixForUsageReport
import dev.westelh.obe.client.objectstorage.RequestFactory

class SimpleRequestFactory(private val targetTenantId: String) : RequestFactory {
    override fun buildListCostReportsRequest(start: String): ListObjectsRequest {
        return ListObjectsRequest.builder()
            .billingNamespace()
            .billingBucketName(targetTenantId)
            .billingPrefixForCostReport()
            .start(start)
            .build()
    }

    override fun buildListUsageReportsRequest(): ListObjectsRequest {
        return ListObjectsRequest.builder()
            .billingNamespace()
            .billingBucketName(targetTenantId)
            .billingPrefixForUsageReport()
            .build()
    }

    override fun buildGetReportRequest(objectName: String): GetObjectRequest {
        return GetObjectRequest.builder()
            .billingNamespace()
            .objectName(objectName)
            .build()
    }
}