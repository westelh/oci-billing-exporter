package dev.westelh.oci.billing.exporter.app

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.time.Duration

class Config {
    @JsonProperty("target")
    val targetTenantId: String = ""

    val server: ServerConfig = ServerConfig()
    class ServerConfig {
        val port: Int = 2112
        val inetAddress: String = ""
        val hostname: String = ""
        val interval: Long = 21600000L
        val intervalOnError: Long = 10000L

        val downloadConfig: DownloadConfig = DownloadConfig()
        class DownloadConfig {
            // Maximum number of retries, not including the initial attempt.
            val maxRetries: Int = 3

            // The size in bytes of the individual parts as which the object is downloaded.
            val partSizeInBytes: Int = 1500 // MTU

            // The number of parallel operations to perform when downloading an object in multiple parts.
            // Decreasing this value will make multipart downloads less resource intensive,but they may take longer.
            // Increasing this value may improve download times,
            // but the download process will consume more system resources and network bandwidth.
            val parallelDownloads: Int = 5
            // The threshold size in bytes at which we will start splitting the object into parts.
            val multipartDownloadThresholdInBytes: Long = 1 * 1024 * 1024  // 1MB

            // Initial backoff, before a retry is performed.
            val initialBackoff: Duration = Duration.ofSeconds(3)
            // Maximum backoff between retries
            val maxBackoff: Duration = Duration.ofHours(24)
        }
    }

    val auth: AuthConfig = AuthConfig()
    class AuthConfig {
        @JsonProperty("instancePrincipal")
        val instancePrincipal: InstancePrincipalConfig? = null
        class InstancePrincipalConfig {
            val timeout: Int = 10
            val retries: Int = 3
        }

        @JsonProperty("resourcePrincipal")
        val resourcePrincipal: ResourcePrincipalConfig? = null
        class ResourcePrincipalConfig {
            val timeout: Int = 10
            val retries: Int = 3
        }

        val config: FileConfig = FileConfig()
        class FileConfig {
            val path: String = ""
            val profile: String = ""
        }
    }
}

fun configFromYamlFile(file: File): Config {
    val mapper = YAMLMapper().registerKotlinModule()
    return mapper.readValue(file)
}
