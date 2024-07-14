package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import java.io.InputStream
import java.util.zip.GZIPInputStream

interface CostReportService {
    fun getObjectByName(name: String): GetObjectResponse

    // Cost report is stored as gzip compressed file on the server
    fun downloadObjectByName(name: String): InputStream = GZIPInputStream(getObjectByName(name).inputStream)

    fun listAllCostReports(tenantId: String): MutableIterable<ObjectSummary>
}