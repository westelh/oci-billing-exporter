package dev.westelh.oci.billing.exporter.api

import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse

interface ObjectDownloader {
    fun download(request: GetObjectRequest): Result<GetObjectResponse>
}