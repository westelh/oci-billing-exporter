package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient

class CachedObjectStorage(adp: AuthenticationDetailsProvider) : ObjectStorageFactory {
    private val os: ObjectStorageAsyncClient = ObjectStorageAsyncClient.builder().build(adp)
    override fun getObjectStorageAsync(): ObjectStorageAsync {
        return os
    }
}