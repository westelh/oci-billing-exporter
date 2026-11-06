package dev.westelh.obe.config

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider

fun loadResourcePrincipalConfig(config: Config.AuthConfig.ResourcePrincipalConfig): ResourcePrincipalAuthenticationDetailsProvider {
    return ResourcePrincipalAuthenticationDetailsProvider.builder()
        // The SDK passes this value to Duration.ofMillis() as the connect timeout of each request
        // to the instance metadata service, so it has to be given in milliseconds.
        .timeoutForEachRetry(Math.toIntExact(config.timeout.toMillis()))
        .detectEndpointRetries(config.retries)
        .build()
}