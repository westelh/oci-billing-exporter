package dev.westelh.obe.config

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder

fun loadResourcePrincipalConfig(config: Config.AuthConfig.ResourcePrincipalConfig): ResourcePrincipalAuthenticationDetailsProvider =
    resourcePrincipalsBuilder(config).build()

internal fun resourcePrincipalsBuilder(config: Config.AuthConfig.ResourcePrincipalConfig): ResourcePrincipalAuthenticationDetailsProviderBuilder =
    ResourcePrincipalAuthenticationDetailsProvider.builder()
        // The SDK passes this value to Duration.ofMillis() as the connect timeout of each request
        // to the instance metadata service, so it has to be given in milliseconds.
        .timeoutForEachRetry(Math.toIntExact(config.timeout.toMillis()))
        .detectEndpointRetries(config.retries)
