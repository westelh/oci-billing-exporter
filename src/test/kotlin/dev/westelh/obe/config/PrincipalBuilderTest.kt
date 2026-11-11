package dev.westelh.obe.config

import com.oracle.bmc.auth.AbstractFederationClientAuthenticationDetailsProviderBuilder
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

// Only checks the values handed to the SDK builders; nothing here builds a provider, so nothing
// talks to the instance metadata service or the auth service.
class PrincipalBuilderTest : StringSpec({
    "passes the instance principal timeout in milliseconds and the retries as they are" {
        val config = Config.fromYaml(
            """
            target: ocid1.tenancy.oc1..test
            auth:
              instancePrincipal:
                timeout: PT10S
                retries: 5
            """.trimIndent().byteInputStream(),
        ).auth.instancePrincipal!!

        val builder = instancePrincipalsBuilder(config)

        builder.intField("timeoutForEachRetry") shouldBe 10_000
        builder.intField("detectEndpointRetries") shouldBe 5
    }

    "passes the resource principal timeout in milliseconds and the retries as they are" {
        val config = Config.fromYaml(
            """
            target: ocid1.tenancy.oc1..test
            auth:
              resourcePrincipal:
                timeout: PT10S
                retries: 5
            """.trimIndent().byteInputStream(),
        ).auth.resourcePrincipal!!

        val builder = resourcePrincipalsBuilder(config)

        builder.intField("timeoutForEachRetry") shouldBe 10_000
        builder.intField("detectEndpointRetries") shouldBe 5
    }
})

/** The SDK builders keep these settings in protected fields without getters. */
private fun Any.intField(name: String): Int =
    AbstractFederationClientAuthenticationDetailsProviderBuilder::class.java.getDeclaredField(name)
        .apply { isAccessible = true }
        .getInt(this)
