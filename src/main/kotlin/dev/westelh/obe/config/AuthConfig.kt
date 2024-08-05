package dev.westelh.obe.config

import com.oracle.bmc.auth.AuthenticationDetailsProvider

fun Config.AuthConfig.anythingAvailable(): AuthenticationDetailsProvider? {
    runCatching { loadInstancePrincipalConfig(instancePrincipal!!) }.onSuccess { return it as AuthenticationDetailsProvider }
    runCatching { loadResourcePrincipalConfig(resourcePrincipal!!) }.onSuccess { return it as AuthenticationDetailsProvider }
    runCatching { loadFileConfig(config) }.onSuccess { return it }
    return null
}