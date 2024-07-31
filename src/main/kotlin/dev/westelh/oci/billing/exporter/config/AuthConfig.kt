package dev.westelh.oci.billing.exporter.config

import com.oracle.bmc.auth.AuthenticationDetailsProvider

fun loadAuthConfig(config: Config.AuthConfig): Result<AuthenticationDetailsProvider> = runCatching {
    if (config.instancePrincipal != null) {
//        logger.atInfo().log("Authenticating instance principal")
        loadInstancePrincipalConfig(config.instancePrincipal)
    }
    if (config.resourcePrincipal != null) {
//        logger.atInfo().log("Authenticating resource principal")
        loadResourcePrincipalConfig(config.resourcePrincipal)
    }
//    logger.atInfo().log("Authenticating by oci config file")
    loadFileConfig(config.config)
}

fun readAuthenticationDetailsProviderFrom(config: Config.AuthConfig): AuthenticationDetailsProvider? {
    runCatching { loadInstancePrincipalConfig(config.instancePrincipal!!) }.onSuccess { return it as AuthenticationDetailsProvider }
    runCatching { loadResourcePrincipalConfig(config.resourcePrincipal!!) }.onSuccess { return it as AuthenticationDetailsProvider }
    runCatching { loadFileConfig(config.config) }.onSuccess { return it }
    return null
}