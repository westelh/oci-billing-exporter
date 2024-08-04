package dev.westelh.obe.config

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.time.Duration

class Config {
    companion object {
        private val mapper = YAMLMapper().registerModules(kotlinModule(), JavaTimeModule())
        fun fromYaml(file: File): Config = mapper.readValue(file)
        fun fromYaml(inputStream: InputStream): Config = mapper.readValue(inputStream)
    }

    @JsonProperty(value = "target", required = true)
    @JsonPropertyDescription("The ID of target tenancy which this app get metrics from.")
    val targetTenantId: String = ""

    @JsonPropertyDescription("server defines how to run server.")
    val server: ServerConfig = ServerConfig()

    class ServerConfig {
        @JsonProperty(defaultValue = "2112")
        @JsonPropertyDescription("Server port")
        val port: Int = 2112

        @JsonProperty(defaultValue = "127.0.0.1")
        @JsonPropertyDescription("Host name or address to bind the server.")
        val host: InetAddress = InetAddress.getByName("127.0.0.1")

        @JsonProperty(defaultValue = "PT6H")
        @JsonPropertyDescription("How long server delays after an update. Specify in ISO-8601 format.")
        val delay: Duration = Duration.parse("PT6H")

        @JsonPropertyDescription("Defines how server download cost and usage reports.")
        val download: DownloadConfig = DownloadConfig()

        class DownloadConfig {
            @JsonProperty(defaultValue = "3")
            @JsonPropertyDescription("Maximum number of retries, not including the initial attempt.")
            val maxRetries: Int = 3

            @JsonProperty(value="partSize", defaultValue = "4194304")
            @JsonPropertyDescription("The size in bytes of the individual parts as which the object is downloaded.")
            val partSizeInBytes: Int = 4194304

            @JsonProperty(defaultValue = "5")
            @JsonPropertyDescription("The number of parallel operations to perform when downloading an object in multiple parts.")
            // Decreasing this value will make multipart downloads less resource intensive,but they may take longer.
            // Increasing this value may improve download times,
            // but the download process will consume more system resources and network bandwidth.
            val parallelDownloads: Int = 5

            @JsonProperty(value="multipartThreshold", defaultValue = "4194304")
            @JsonPropertyDescription("The threshold size in bytes at which we will start splitting the object into parts.")
            val multipartDownloadThresholdInBytes: Long = 4194304

            @JsonProperty(defaultValue = "PT3S")
            @JsonPropertyDescription("Initial backoff, before a retry is performed. Specify in ISO-8601 format.")
            val initialBackoff: Duration = Duration.parse("PT3S")

            @JsonProperty(defaultValue = "P1D")
            @JsonPropertyDescription("Maximum backoff between retries. Specify in ISO-8601 format.")
            val maxBackoff: Duration = Duration.parse("P1D")
        }
    }

    @JsonPropertyDescription("The authentication method for OCI api calls.")
    val auth: AuthConfig = AuthConfig()

    class AuthConfig {
        @JsonProperty("instancePrincipal")
        @JsonPropertyDescription("Configurations for authentication by instance principals.")
        val instancePrincipal: InstancePrincipalConfig? = null

        @JsonClassDescription("Configuration schema for authentication by instance principals.")
        class InstancePrincipalConfig {
            @JsonProperty(defaultValue = "PT10S")
            @JsonPropertyDescription("How long application can wait for completion of authentication.")
            val timeout: Duration = Duration.parse("PT10S")

            @JsonProperty(defaultValue = "3")
            @JsonPropertyDescription("How many times application tries to authenticate when it fails.")
            val retries: Int = 3
        }

        @JsonProperty("resourcePrincipal")
        @JsonPropertyDescription("Configurations for authentication by resource principals.")
        val resourcePrincipal: ResourcePrincipalConfig? = null

        @JsonClassDescription("Configuration schema for authentication by resource principals.")
        class ResourcePrincipalConfig {
            @JsonProperty(defaultValue = "PT10S")
            @JsonPropertyDescription("How long application can wait for completion of authentication.")
            val timeout: Duration = Duration.parse("PT10S")

            @JsonProperty(defaultValue = "3")
            @JsonPropertyDescription("How many times application tries to authenticate when it fails.")
            val retries: Int = 3
        }

        @JsonPropertyDescription("Configurations for authentication by config file.")
        val config: FileConfig = FileConfig()

        @JsonClassDescription("Configuration schema for authentication by config files.")
        class FileConfig {
            @JsonPropertyDescription("The path to config file.")
            val path: String = ""

            @JsonProperty(defaultValue = "DEFAULT")
            @JsonPropertyDescription("The profile to use in config file.")
            val profile: String = "DEFAULT"
        }
    }
}
