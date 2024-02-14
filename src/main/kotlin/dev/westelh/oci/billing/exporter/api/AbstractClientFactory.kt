package dev.westelh.oci.billing.exporter.api

import com.oracle.bmc.objectstorage.ObjectStorageClient

interface AbstractClientFactory {
    fun createClient(): ObjectStorageClient
}