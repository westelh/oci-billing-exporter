package dev.westelh.obe.client

import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest

interface RequestFactory {
    fun buildListCostReportsRequest(start: String = ""): ListObjectsRequest
    fun buildListUsageReportsRequest(): ListObjectsRequest
    fun buildGetReportRequest(objectName: String): GetObjectRequest
}