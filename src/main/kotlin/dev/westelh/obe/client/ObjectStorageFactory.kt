package dev.westelh.obe.client

import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageAsync

interface ObjectStorageFactory {
    fun getObjectStorage(): ObjectStorage
    fun getObjectStorageAsync(): ObjectStorageAsync
}