package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageAsync
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient

class OnDemandObjectStorage(private val adp: AuthenticationDetailsProvider) : ObjectStorageFactory {
    override fun getObjectStorageAsync(): ObjectStorageAsync {
        return ObjectStorageAsyncClient.builder().build(adp)
    }
}