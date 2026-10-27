package dev.westelh.obe.client.objectstorage

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import com.oracle.bmc.objectstorage.transfer.DownloadConfiguration
import com.oracle.bmc.objectstorage.transfer.DownloadManager

// Object Storage operations needed to fetch billing reports
interface BillingObjectStorage {
    suspend fun listObjects(request: ListObjectsRequest): ListObjectsResponse
    suspend fun getObject(request: GetObjectRequest): GetObjectResponse
}

class OciBillingObjectStorage(
    adp: AbstractAuthenticationDetailsProvider,
    downloadConfiguration: DownloadConfiguration
) : BillingObjectStorage {
    private val objectStorageAsync: ObjectStorageAsync = ObjectStorageAsyncClient.builder().build(adp)
    private val downloadManager: DownloadManager =
        DownloadManager(ObjectStorageClient.builder().build(adp), downloadConfiguration)

    override suspend fun listObjects(request: ListObjectsRequest): ListObjectsResponse =
        objectStorageAsync.suspendListObjects(request)

    override suspend fun getObject(request: GetObjectRequest): GetObjectResponse =
        downloadManager.suspendGetObject(request)
}
