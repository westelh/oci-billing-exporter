package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.*

private val logger: FluentLogger = FluentLogger.forEnclosingClass()

fun authByInstancePrincipals(config: Config.AuthConfig.InstancePrincipalConfig): InstancePrincipalsAuthenticationDetailsProvider {
    return InstancePrincipalsAuthenticationDetailsProvider.builder()
        .timeoutForEachRetry(config.timeout)
        .detectEndpointRetries(config.retries)
        .build()
}

fun authByResourcePrincipals(config: Config.AuthConfig.ResourcePrincipalConfig): ResourcePrincipalAuthenticationDetailsProvider {
    return ResourcePrincipalAuthenticationDetailsProvider.builder()
        .timeoutForEachRetry(config.timeout)
        .detectEndpointRetries(config.retries)
        .build()
}

fun authByConfigFile(config: Config.AuthConfig.FileConfig): ConfigFileAuthenticationDetailsProvider {
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

fun auth(config: Config.AuthConfig): Result<AuthenticationDetailsProvider> = runCatching {
    if (config.instancePrincipal != null) {
        logger.atInfo().log("Authenticating instance principal")
        authByInstancePrincipals(config.instancePrincipal)
    }
    if (config.resourcePrincipal != null) {
        logger.atInfo().log("Authenticating resource principal")
        authByResourcePrincipals(config.resourcePrincipal)
    }
    logger.atInfo().log("Authenticating by oci config file")
    authByConfigFile(config.config)
}
