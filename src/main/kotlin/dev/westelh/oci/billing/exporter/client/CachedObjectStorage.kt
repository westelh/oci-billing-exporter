package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient

class CachedObjectStorage(adp: AuthenticationDetailsProvider) : ObjectStorageFactory {
    val os = ObjectStorageAsyncClient.builder().build(adp)
    override fun getObjectStorageAsync(): ObjectStorageAsync {
        return os
    }
}