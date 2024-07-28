package dev.westelh.oci.billing.exporter.app

import com.oracle.bmc.objectstorage.transfer.DownloadConfiguration

fun buildDownloadConfiguration(config: Config.ServerConfig.DownloadConfig): DownloadConfiguration {
    return DownloadConfiguration.builder()
        .apply {
            multipartDownloadThresholdInBytes(config.multipartDownloadThresholdInBytes)
            initialBackoff(config.initialBackoff)
            maxBackoff(config.maxBackoff)
            maxRetries(config.maxRetries)
            parallelDownloads(config.parallelDownloads)
            partSizeInBytes(config.partSizeInBytes)

            // executor service can be set
            // but current config can't produce executor service
            // executorService()
        }
        .build()
}