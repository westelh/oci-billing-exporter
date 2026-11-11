package dev.westelh.obe.config

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider.InstancePrincipalsAuthenticationDetailsProviderBuilder

fun loadInstancePrincipalConfig(config: Config.AuthConfig.InstancePrincipalConfig): InstancePrincipalsAuthenticationDetailsProvider =
    instancePrincipalsBuilder(config).build()

internal fun instancePrincipalsBuilder(config: Config.AuthConfig.InstancePrincipalConfig): InstancePrincipalsAuthenticationDetailsProviderBuilder =
    InstancePrincipalsAuthenticationDetailsProvider.builder()
        // The SDK passes this value to Duration.ofMillis() as the connect timeout of each request
        // to the instance metadata service, so it has to be given in milliseconds.
        .timeoutForEachRetry(Math.toIntExact(config.timeout.toMillis()))
        .detectEndpointRetries(config.retries)
