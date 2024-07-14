package dev.westelh.oci.billing.exporter.app.client

import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStoragePaginators
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.transfer.DownloadConfiguration
import com.oracle.bmc.objectstorage.transfer.DownloadManager

class ServiceImpl(client: ObjectStorage, downloadConfiguration: DownloadConfiguration) : Service {
    constructor(client: ObjectStorage) : this(client, DownloadConfiguration.builder().build())

    private val objectStoragePaginators = ObjectStoragePaginators(client)
    private val downloadManager = DownloadManager(client, downloadConfiguration)

    override fun iterateObjects(request: ListObjectsRequest): Result<MutableIterable<ObjectSummary>> {
        return kotlin.runCatching {
            objectStoragePaginators.listObjectsRecordIterator(request)
        }
    }

    override fun downloadObject(request: GetObjectRequest): Result<GetObjectResponse> {
        return kotlin.runCatching {
            downloadManager.getObject(request)
        }
    }
}