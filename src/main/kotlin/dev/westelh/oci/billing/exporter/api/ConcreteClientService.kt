package dev.westelh.oci.billing.exporter.api

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.ObjectStoragePaginators
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse

class ConcreteClientService(client: ObjectStorageClient) : ClientService {
    private val objectStoragePaginators = ObjectStoragePaginators(client)

    override fun iterateObjects(request: ListObjectsRequest): Result<MutableIterable<ObjectSummary>> {
        return kotlin.runCatching {
            objectStoragePaginators.listObjectsRecordIterator(request)
        }
    }
}