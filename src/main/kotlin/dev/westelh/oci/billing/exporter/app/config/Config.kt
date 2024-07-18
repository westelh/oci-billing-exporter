package dev.westelh.oci.billing.exporter.app.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import dev.westelh.oci.billing.exporter.app.AbstractADP
import java.io.File

class Config {
    @JsonProperty("auth")
    val authMethod: String = ""
}

fun configFromYamlFile(file: File): Config {
    val mapper = YAMLMapper().registerKotlinModule()
    return mapper.readValue(file)
}

fun loadAuth(config: Config): AbstractADP {
    return InstancePrincipalsAuthenticationDetailsProvider.builder().build()
}