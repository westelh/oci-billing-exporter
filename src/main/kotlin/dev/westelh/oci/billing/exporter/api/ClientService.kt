package dev.westelh.oci.billing.exporter.api

import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse

interface ClientService {
    fun iterateObjects(request: ListObjectsRequest): Result<MutableIterable<ObjectSummary>>
}