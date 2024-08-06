package dev.westelh.obe.client.objectstorage

import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import dev.westelh.obe.client.awaitInCommonPool
import java.util.concurrent.CompletableFuture

suspend fun ObjectStorageAsync.suspendListObjects(request: ListObjectsRequest): ListObjectsResponse {
    return listObjects(request, null).awaitInCommonPool()
}

suspend fun DownloadManager.suspendGetObject(request: GetObjectRequest): GetObjectResponse {
    return CompletableFuture.supplyAsync { getObject(request) }.awaitInCommonPool()
}