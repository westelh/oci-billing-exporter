package dev.westelh.oci.billing.exporter.app

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

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
