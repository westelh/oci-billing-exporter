package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse

interface Service {
    fun iterateObjects(request: ListObjectsRequest): Result<MutableIterable<ObjectSummary>>
    fun downloadObject(request: GetObjectRequest): Result<GetObjectResponse>
}