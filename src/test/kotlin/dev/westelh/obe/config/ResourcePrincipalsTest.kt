package dev.westelh.obe.config

import com.oracle.bmc.Region
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider.ClaimKeys
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.time.Instant
import java.util.Base64
import kotlin.system.exitProcess

// The resource principal provider is configured only through environment variables, which cannot be
// changed inside a running JVM. Each case therefore runs loadResourcePrincipalConfig in a child JVM
// whose environment contains nothing but the variables given here.
class ResourcePrincipalsTest : StringSpec({
    "builds a v2.2 provider from an RPST and PEM passed as values, without network access" {
        val pem = generateRsaKeyPair().privateKeyPem()
        val rpst = fakeRpst()

        val result = runInChildJvm(
            ResourcePrincipalProbe::class,
            env = mapOf(
                "OCI_RESOURCE_PRINCIPAL_VERSION" to "2.2",
                "OCI_RESOURCE_PRINCIPAL_RPST" to rpst,
                "OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM" to pem,
                "OCI_RESOURCE_PRINCIPAL_REGION" to "ap-tokyo-1",
            ),
        )

        withClue(result.toString()) {
            result.exitCode shouldBe 0
            result.values["region"] shouldBe Region.AP_TOKYO_1.regionId
            result.values["tenantId"] shouldBe TENANT_ID
            result.values["compartmentId"] shouldBe COMPARTMENT_ID
            result.values["keyId"] shouldBe "ST\$$rpst"
            result.values["refreshedKeyId"] shouldBe "ST\$$rpst"
            result.values["hasPrivateKey"] shouldBe "true"
        }
    }

    "builds a v2.2 provider from an RPST and PEM passed as file paths, without network access" {
        val dir = tempdir()
        val rpst = fakeRpst()
        val rpstFile = File(dir, "rpst").apply { writeText(rpst) }
        val pemFile = File(dir, "private.pem").apply { writeText(generateRsaKeyPair().privateKeyPem()) }

        val result = runInChildJvm(
            ResourcePrincipalProbe::class,
            env = mapOf(
                "OCI_RESOURCE_PRINCIPAL_VERSION" to "2.2",
                "OCI_RESOURCE_PRINCIPAL_RPST" to rpstFile.path,
                "OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM" to pemFile.path,
                "OCI_RESOURCE_PRINCIPAL_REGION" to "us-ashburn-1",
            ),
        )

        withClue(result.toString()) {
            result.exitCode shouldBe 0
            result.values["region"] shouldBe Region.US_ASHBURN_1.regionId
            result.values["tenantId"] shouldBe TENANT_ID
            result.values["keyId"] shouldBe "ST\$$rpst"
            result.values["hasPrivateKey"] shouldBe "true"
        }
    }

    "fails when OCI_RESOURCE_PRINCIPAL_VERSION is not set" {
        val result = runInChildJvm(ResourcePrincipalProbe::class)

        withClue(result.toString()) {
            result.exitCode shouldBe 1
            result.values["error"] shouldContain "OCI_RESOURCE_PRINCIPAL_VERSION environment variable missing"
        }
    }

    "fails when the v2.2 RPST is not set" {
        val result = runInChildJvm(
            ResourcePrincipalProbe::class,
            env = mapOf(
                "OCI_RESOURCE_PRINCIPAL_VERSION" to "2.2",
                "OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM" to generateRsaKeyPair().privateKeyPem(),
                "OCI_RESOURCE_PRINCIPAL_REGION" to "ap-tokyo-1",
            ),
        )

        withClue(result.toString()) {
            result.exitCode shouldBe 1
            result.values["error"] shouldContain "OCI_RESOURCE_PRINCIPAL_RPST"
        }
    }
})

private const val TENANT_ID = "ocid1.tenancy.oc1..resourceprincipaltest"
private const val COMPARTMENT_ID = "ocid1.compartment.oc1..resourceprincipaltest"

/**
 * An unsigned resource principal session token. The SDK only decodes the claims and does not verify
 * the signature, but it rejects tokens whose signature part is empty, hence the placeholder.
 */
private fun fakeRpst(): String {
    val now = Instant.now().epochSecond
    val header = """{"alg":"none","typ":"JWT"}"""
    val payload = """
        {"sub":"ocid1.fnfunc.oc1..resourceprincipaltest",
         "res_type":"fnfunc",
         "${ClaimKeys.TENANT_ID_CLAIM_KEY}":"$TENANT_ID",
         "${ClaimKeys.COMPARTMENT_ID_CLAIM_KEY}":"$COMPARTMENT_ID",
         "iat":$now,
         "exp":${now + 3600}}
    """.trimIndent()
    val encoder = Base64.getUrlEncoder().withoutPadding()
    return listOf(header, payload, "unsigned").joinToString(".") { encoder.encodeToString(it.toByteArray()) }
}

/** Entry point of the child JVM: builds the provider from the environment and prints what it got. */
object ResourcePrincipalProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        forbidNetworkAccess()
        try {
            val provider = loadResourcePrincipalConfig(Config.AuthConfig.ResourcePrincipalConfig())
            println("region=${provider.region.regionId}")
            println("tenantId=${provider.getStringClaim(ClaimKeys.TENANT_ID_CLAIM_KEY)}")
            println("compartmentId=${provider.getStringClaim(ClaimKeys.COMPARTMENT_ID_CLAIM_KEY)}")
            println("keyId=${provider.keyId}")
            provider.refresh()
            println("refreshedKeyId=${provider.keyId}")
            println("hasPrivateKey=${provider.privateKey.use { it.read() != -1 }}")
        } catch (e: Throwable) {
            println("error=${e::class.java.name}: ${e.message?.lineSequence()?.first()}")
            exitProcess(1)
        }
    }
}
