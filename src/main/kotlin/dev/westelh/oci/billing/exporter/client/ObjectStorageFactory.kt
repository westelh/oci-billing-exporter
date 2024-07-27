package dev.westelh.oci.billing.exporter.client

import com.oracle.bmc.objectstorage.ObjectStorageAsync

interface ObjectStorageFactory {
    fun getObjectStorageAsync(): ObjectStorageAsync
}