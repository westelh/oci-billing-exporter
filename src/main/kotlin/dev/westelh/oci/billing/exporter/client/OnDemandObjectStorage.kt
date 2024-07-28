package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient

class OnDemandObjectStorage(private val adp: AuthenticationDetailsProvider) : ObjectStorageFactory {
    override fun getObjectStorage(): ObjectStorage {
        return ObjectStorageClient.builder().build(adp)
    }

    override fun getObjectStorageAsync(): ObjectStorageAsync {
        return ObjectStorageAsyncClient.builder().build(adp)
    }
}