package dev.westelh.oci.billing.exporter.app

import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest

interface RequestFactory {
    fun buildListCostReportsRequest(start: String = ""): ListObjectsRequest
    fun buildListUsageReportsRequest(): ListObjectsRequest
    fun buildGetReportRequest(objectName: String): GetObjectRequest
}