package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.objectstorage.ObjectStoragePaginators
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.transfer.DownloadConfiguration
import com.oracle.bmc.objectstorage.transfer.DownloadManager

class ServiceImpl(tenantId: String, objectStorageFactory: ObjectStorageFactory) : Service {
    private val objectStorage = objectStorageFactory.createObjectStorage()
    private val objectStoragePaginators = ObjectStoragePaginators(objectStorage)
    private val downloadConfiguration = DownloadConfiguration.builder().build()
    private val downloadManager = DownloadManager(objectStorage, downloadConfiguration)
    private val requestFactory = RequestFactory(tenantId)

    private fun iterateObjects(request: ListObjectsRequest): MutableIterable<ObjectSummary> {
        return objectStoragePaginators.listObjectsRecordIterator(request)
    }

    override fun downloadObjectByName(request: GetObjectRequest): GetObjectResponse {
        return downloadManager.getObject(request)
    }

    override fun listAllCostReports(tenantId: String): MutableIterable<ObjectSummary> {
        val request = requestFactory.createListCostReportsRequest()
        return iterateObjects(request)
    }
}