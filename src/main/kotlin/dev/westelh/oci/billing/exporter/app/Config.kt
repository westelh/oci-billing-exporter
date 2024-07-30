package dev.westelh.oci.billing.exporter.app

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.flogger.FluentLogger
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration

private val logger: FluentLogger = FluentLogger.forEnclosingClass()

class Config {
    @JsonProperty("target")
    @JsonPropertyDescription("The ID of target tenancy which this app get metrics from.")
    val targetTenantId: String = ""

    @JsonPropertyDescription("server defines how to run server.")
    val server: ServerConfig = ServerConfig()

    class ServerConfig {
        @JsonProperty(defaultValue = "2112")
        @JsonPropertyDescription("Server port")
        val port: Int = 2112

        @JsonProperty(defaultValue = "127.0.0.1")
        @JsonPropertyDescription("Inet address to bind the server. Priority is higher than hostname.")
        val inetAddress: String = "127.0.0.1"

        @JsonProperty(defaultValue = "localhost")
        @JsonPropertyDescription("Hostname to bind the server. Priority is lower than inetAddress.")
        val hostname: String = "localhost"

        @JsonProperty(defaultValue = "21600000")
        @JsonPropertyDescription("How long server delays after an update in milliseconds.")
        val interval: Long = 21600000L

        @JsonPropertyDescription("Defines how server download cost and usage reports.")
        val download: DownloadConfig = DownloadConfig()

        class DownloadConfig {
            @JsonProperty(defaultValue = "3")
            @JsonPropertyDescription("Maximum number of retries, not including the initial attempt.")
            val maxRetries: Int = 3

            @JsonProperty(defaultValue = "4194304")
            @JsonPropertyDescription("The size in bytes of the individual parts as which the object is downloaded.")
            val partSizeInBytes: Int = 4194304

            @JsonProperty(defaultValue = "5")
            @JsonPropertyDescription("The number of parallel operations to perform when downloading an object in multiple parts.")
            // Decreasing this value will make multipart downloads less resource intensive,but they may take longer.
            // Increasing this value may improve download times,
            // but the download process will consume more system resources and network bandwidth.
            val parallelDownloads: Int = 5

            @JsonProperty(defaultValue = "4194304")
            @JsonPropertyDescription("The threshold size in bytes at which we will start splitting the object into parts.")
            val multipartDownloadThresholdInBytes: Long = 4194304

            @JsonProperty(defaultValue = "3")
            @JsonPropertyDescription("Initial backoff, before a retry is performed.")
            val initialBackoff: Duration = Duration.ofSeconds(3)

            @JsonProperty(defaultValue = "24")
            @JsonPropertyDescription("Maximum backoff between retries.")
            val maxBackoff: Duration = Duration.ofHours(24)
        }
    }

    @JsonPropertyDescription("The authentication method for OCI api calls.")
    val auth: AuthConfig = AuthConfig()

    class AuthConfig {
        @JsonProperty("instancePrincipal")
        @JsonPropertyDescription("A set of configuration for authentication by instance principals.")
        val instancePrincipal: InstancePrincipalConfig? = null

        class InstancePrincipalConfig {
            @JsonProperty(defaultValue = "10")
            @JsonPropertyDescription("How long application can wait for completion of authentication.")
            val timeout: Int = 10

            @JsonProperty(defaultValue = "3")
            @JsonPropertyDescription("How many times application tries to authenticate when it fails.")
            val retries: Int = 3
        }

        @JsonProperty("resourcePrincipal")
        @JsonPropertyDescription("A set of configuration for authentication by resource principals.")
        val resourcePrincipal: ResourcePrincipalConfig? = null

        class ResourcePrincipalConfig {
            @JsonProperty(defaultValue = "10")
            @JsonPropertyDescription("How long application can wait for completion of authentication.")
            val timeout: Int = 10

            @JsonProperty(defaultValue = "3")
            @JsonPropertyDescription("How many times application tries to authenticate when it fails.")
            val retries: Int = 3
        }

        @JsonPropertyDescription("A set of configuration for authentication by config file.")
        val config: FileConfig = FileConfig()

        class FileConfig {
            @JsonPropertyDescription("The path to config file.")
            val path: String = ""

            @JsonProperty(defaultValue = "DEFAULT")
            @JsonPropertyDescription("The profile to use in config file.")
            val profile: String = "DEFAULT"
        }
    }
}

fun configFromYamlFile(file: File): Config {
    val mapper = YAMLMapper().registerKotlinModule()
    return mapper.readValue(file)
}

fun readInetAddressFromConfig(config: Config.ServerConfig): InetAddress? {
    return try {
        InetAddress.getByName(config.inetAddress)
    } catch (e: UnknownHostException) {
        logger.atSevere().withCause(e).log("inetAddress %s is not a valid address.")
        null
    }
}
