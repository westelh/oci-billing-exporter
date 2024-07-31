package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider

private val logger: FluentLogger = FluentLogger.forEnclosingClass()

fun loadInstancePrincipalConfig(config: Config.AuthConfig.InstancePrincipalConfig): InstancePrincipalsAuthenticationDetailsProvider {
    return InstancePrincipalsAuthenticationDetailsProvider.builder()
        // Documentation of AbstractFederationClientAuthenticationDetailsProviderBuilder
        // https://docs.oracle.com/en-us/iaas/tools/java/3.45.0/com/oracle/bmc/auth/AbstractFederationClientAuthenticationDetailsProviderBuilder.html#timeoutForEachRetry
        // doesn't say anything about the unit of timeout.
        // Though the Int type is suspicious, using toSeconds() for now.
        .timeoutForEachRetry(config.timeout.toSeconds().toInt())
        .detectEndpointRetries(config.retries)
        .build()
}

fun loadResourcePrincipalConfig(config: Config.AuthConfig.ResourcePrincipalConfig): ResourcePrincipalAuthenticationDetailsProvider {
    return ResourcePrincipalAuthenticationDetailsProvider.builder()
        // Documentation of AbstractFederationClientAuthenticationDetailsProviderBuilder
        // https://docs.oracle.com/en-us/iaas/tools/java/3.45.0/com/oracle/bmc/auth/AbstractFederationClientAuthenticationDetailsProviderBuilder.html#timeoutForEachRetry
        // doesn't say anything about the unit of timeout.
        // Though the Int type is suspicious, using toSeconds() for now.
        .timeoutForEachRetry(config.timeout.toSeconds().toInt())
        .detectEndpointRetries(config.retries)
        .build()
}

fun loadFileConfig(config: Config.AuthConfig.FileConfig): ConfigFileAuthenticationDetailsProvider {
    with(config) {
        val profileNotBlank = profile.ifBlank {
            logger.atInfo().log("No profile is specified, using \"DEFAULT\" instead.")
            "DEFAULT"
        }
        if (path.isNotBlank()) {
            // Path is different from the default
            return ConfigFileAuthenticationDetailsProvider(path, profileNotBlank)
        }
        else return ConfigFileAuthenticationDetailsProvider(profileNotBlank)
    }
}

fun loadAuthConfig(config: Config.AuthConfig): Result<AuthenticationDetailsProvider> = runCatching {
    if (config.instancePrincipal != null) {
        logger.atInfo().log("Authenticating instance principal")
        loadInstancePrincipalConfig(config.instancePrincipal)
    }
    if (config.resourcePrincipal != null) {
        logger.atInfo().log("Authenticating resource principal")
        loadResourcePrincipalConfig(config.resourcePrincipal)
    }
    logger.atInfo().log("Authenticating by oci config file")
    loadFileConfig(config.config)
}
