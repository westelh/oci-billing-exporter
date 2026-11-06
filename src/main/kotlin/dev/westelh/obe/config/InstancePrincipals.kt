package dev.westelh.obe.config

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider.InstancePrincipalsAuthenticationDetailsProviderBuilder

fun loadInstancePrincipalConfig(config: Config.AuthConfig.InstancePrincipalConfig): InstancePrincipalsAuthenticationDetailsProvider =
    loadInstancePrincipalConfig(config, metadataBaseUrl = null, federationEndpoint = null)

/**
 * Builds the provider, optionally pointing it at another instance metadata service and federation
 * endpoint. The overrides exist only so that tests can use local mock servers; they are not
 * exposed as configuration.
 */
internal fun loadInstancePrincipalConfig(
    config: Config.AuthConfig.InstancePrincipalConfig,
    metadataBaseUrl: String?,
    federationEndpoint: String?,
): InstancePrincipalsAuthenticationDetailsProvider =
    instancePrincipalsBuilder(config, metadataBaseUrl, federationEndpoint).build()

internal fun instancePrincipalsBuilder(
    config: Config.AuthConfig.InstancePrincipalConfig,
    metadataBaseUrl: String? = null,
    federationEndpoint: String? = null,
): InstancePrincipalsAuthenticationDetailsProviderBuilder =
    InstancePrincipalsAuthenticationDetailsProvider.builder()
        // The SDK passes this value to Duration.ofMillis() as the connect timeout of each request
        // to the instance metadata service, so it has to be given in milliseconds.
        .timeoutForEachRetry(Math.toIntExact(config.timeout.toMillis()))
        .detectEndpointRetries(config.retries)
        .apply {
            if (metadataBaseUrl != null) metadataBaseUrl(metadataBaseUrl)
            if (federationEndpoint != null) federationEndpoint(federationEndpoint)
        }
