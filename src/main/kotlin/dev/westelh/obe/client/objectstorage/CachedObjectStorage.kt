package dev.westelh.obe.client.objectstorage

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient

class CachedObjectStorage(adp: AuthenticationDetailsProvider) : ObjectStorageFactory {
    private val os = ObjectStorageClient.builder().build(adp)
    private val osAsync: ObjectStorageAsyncClient = ObjectStorageAsyncClient.builder().build(adp)
    override fun getObjectStorage(): ObjectStorage = os
    override fun getObjectStorageAsync(): ObjectStorageAsync = osAsync
}