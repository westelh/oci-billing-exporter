package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.responses.GetObjectResponse

interface Service {
    fun downloadObjectByName(name: String): GetObjectResponse
    fun listAllCostReports(tenantId: String): MutableIterable<ObjectSummary>
}