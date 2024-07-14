package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageClient

interface ObjectStorageFactory {
    fun createObjectStorage(): ObjectStorage
}

class DefaultObjectStorageFactory(private val authenticationDetailsProvider: AbstractAuthenticationDetailsProvider) :
    ObjectStorageFactory {

    override fun createObjectStorage(): ObjectStorage {
        return ObjectStorageClient.builder().build(authenticationDetailsProvider)
    }
}