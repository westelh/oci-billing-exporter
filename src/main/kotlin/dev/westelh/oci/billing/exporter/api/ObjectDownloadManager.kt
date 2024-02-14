package dev.westelh.oci.billing.exporter.api

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.transfer.DownloadConfiguration
import com.oracle.bmc.objectstorage.transfer.DownloadManager

class ObjectDownloadManager(client: ObjectStorageClient, config: DownloadConfiguration): ObjectDownloader {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }
    private val downloadManager = DownloadManager(client, config)
    override fun download(request: GetObjectRequest): Result<GetObjectResponse> {
        return kotlin.runCatching {
            downloadManager.getObject(request)
        }.onSuccess {
            logger.atInfo().log("Downloaded ${it.contentLength} bytes from storage")
        }
    }
}