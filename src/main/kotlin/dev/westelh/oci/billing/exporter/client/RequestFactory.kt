package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest

class RequestFactory(tenancy: String) {
    private val ociNamespace = "bling"
    private val bucketName = tenancy
    fun createListCostReportsRequest(): ListObjectsRequest = ListObjectsRequest.builder()
        .namespaceName(ociNamespace)
        .bucketName(bucketName)
        .prefix("reports/cost-csv")
        .build()

    fun createGetCostReportRequest(objectName: String): GetObjectRequest = GetObjectRequest.builder()
        .namespaceName(ociNamespace)
        .bucketName(bucketName)
        .objectName(objectName)
        .build()
}