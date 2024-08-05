package dev.westelh.obe.client.objectstorage

import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import com.oracle.bmc.objectstorage.transfer.DownloadManager
import dev.westelh.obe.client.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun listAllObjects(client: ObjectStorageAsync, request: ListObjectsRequest): List<ObjectSummary> {
    val sum = mutableListOf<ObjectSummary>()
    var nextStartWith = ""

    do {
        val nextRequest = ListObjectsRequest.builder().copy(request).start(nextStartWith).build()
        val res = client.suspendListObjects(nextRequest)
        sum.addAll(res.listObjects.objects)
        nextStartWith = res.listObjects.nextStartWith ?: ""
    } while (nextStartWith.isNotBlank())

    return sum
}

suspend fun ObjectStorageAsync.suspendListObjects(request: ListObjectsRequest): ListObjectsResponse {
    return listObjects(request, null).await()
}

// TODO: Make this function cancellable.
suspend fun DownloadManager.suspendGetObject(request: GetObjectRequest): GetObjectResponse =
    suspendCoroutine { continuation ->
        val res = getObject(request)
        continuation.resume(res)
    }