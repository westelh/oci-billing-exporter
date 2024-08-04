package dev.westelh.obe.client.objectstorage

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import dev.westelh.obe.client.objectstorage.ObjectStorageFactory

class OnDemandObjectStorage(private val adp: AuthenticationDetailsProvider) : ObjectStorageFactory {
    override fun getObjectStorage(): ObjectStorage {
        return ObjectStorageClient.builder().build(adp)
    }

    override fun getObjectStorageAsync(): ObjectStorageAsync {
        return ObjectStorageAsyncClient.builder().build(adp)
    }
}