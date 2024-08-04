package dev.westelh.obe.config

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider

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