package dev.westelh.obe.config

import com.oracle.bmc.Region
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.io.IOException

class FileConfigTest : StringSpec({
    "uses the DEFAULT profile of the config file at the given path" {
        val fixture = OciConfigFixture.write(tempdir())

        val provider = loadFileConfig(fileConfigOf(fixture.config.path))

        provider.userId shouldBe "ocid1.user.oc1..default"
        provider.tenantId shouldBe "ocid1.tenancy.oc1..default"
        provider.fingerprint shouldBe DEFAULT_FINGERPRINT
        provider.region shouldBe Region.AP_TOKYO_1
        provider.privateKey.use { it.reader().readText() } shouldBe fixture.privateKeyPem
    }

    "uses the requested profile of the config file" {
        val fixture = OciConfigFixture.write(tempdir())

        val provider = loadFileConfig(fileConfigOf(fixture.config.path, profile = "OTHER"))

        provider.userId shouldBe "ocid1.user.oc1..other"
        provider.tenantId shouldBe "ocid1.tenancy.oc1..other"
        provider.fingerprint shouldBe OTHER_FINGERPRINT
        provider.region shouldBe Region.US_ASHBURN_1
        provider.privateKey.use { it.reader().readText() } shouldBe fixture.privateKeyPem
    }

    "fails when the profile does not exist" {
        val fixture = OciConfigFixture.write(tempdir())

        val error = shouldThrow<IllegalArgumentException> {
            loadFileConfig(fileConfigOf(fixture.config.path, profile = "MISSING"))
        }
        error.message shouldContain "MISSING"
    }

    "fails when the config file does not exist" {
        val missing = File(tempdir(), "no-such-config")

        shouldThrow<IOException> { loadFileConfig(fileConfigOf(missing.path)) }
    }

    "fails on creation when the profile has no key_file entry" {
        val fixture = OciConfigFixture.write(tempdir(), includeKeyFile = false)

        val error = shouldThrow<NullPointerException> { loadFileConfig(fileConfigOf(fixture.config.path)) }
        error.message shouldContain "key_file"
    }

    "fails only when the private key is read if key_file points to a missing file" {
        val dir = tempdir()
        val fixture = OciConfigFixture.write(dir, keyFile = File(dir, "missing.pem").path)

        // The SDK reads key_file lazily, so creating the provider still succeeds.
        val provider = loadFileConfig(fileConfigOf(fixture.config.path))

        val error = shouldThrow<IllegalArgumentException> { provider.privateKey }
        error.message shouldContain "missing.pem"
    }

    "reads ~/.oci/config when no path is given, resolved against an isolated home in a child JVM" {
        val home = tempdir()
        val ociDir = File(home, ".oci").apply { mkdirs() }
        val fixture = OciConfigFixture.write(ociDir, keyFile = "~/.oci/key.pem")

        val result = runInChildJvm(DefaultFileConfigProbe::class, systemProperties = mapOf("user.home" to home.path))

        withClue(result.toString()) {
            result.exitCode shouldBe 0
            result.values["userId"] shouldBe "ocid1.user.oc1..default"
            result.values["tenantId"] shouldBe "ocid1.tenancy.oc1..default"
            result.values["region"] shouldBe Region.AP_TOKYO_1.regionId
            result.values["privateKeyHash"] shouldBe fixture.privateKeyPem.hashCode().toString()
        }
    }
})

private const val DEFAULT_FINGERPRINT = "11:11:11:11:11:11:11:11:11:11:11:11:11:11:11:11"
private const val OTHER_FINGERPRINT = "22:22:22:22:22:22:22:22:22:22:22:22:22:22:22:22"

/** A config file with `[DEFAULT]` and `[OTHER]` profiles and a freshly generated key in [dir]. */
private class OciConfigFixture(val config: File, val privateKeyPem: String) {
    companion object {
        fun write(dir: File, includeKeyFile: Boolean = true, keyFile: String? = null): OciConfigFixture {
            val pem = generateRsaKeyPair().privateKeyPem()
            val key = File(dir, "key.pem").apply { writeText(pem) }
            val keyFileLine = if (includeKeyFile) "key_file=${keyFile ?: key.path}" else ""
            val config = File(dir, "config").apply {
                writeText(
                    """
                    [DEFAULT]
                    user=ocid1.user.oc1..default
                    fingerprint=$DEFAULT_FINGERPRINT
                    tenancy=ocid1.tenancy.oc1..default
                    region=ap-tokyo-1
                    $keyFileLine

                    [OTHER]
                    user=ocid1.user.oc1..other
                    fingerprint=$OTHER_FINGERPRINT
                    tenancy=ocid1.tenancy.oc1..other
                    region=us-ashburn-1
                    $keyFileLine
                    """.trimIndent()
                )
            }
            return OciConfigFixture(config, pem)
        }
    }
}

/** Entry point of the child JVM: loads the config through the "no path" branch and prints what it got. */
object DefaultFileConfigProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        forbidNetworkAccess()
        val provider = loadFileConfig(Config.AuthConfig.FileConfig())
        println("userId=${provider.userId}")
        println("tenantId=${provider.tenantId}")
        println("region=${provider.region.regionId}")
        println("privateKeyHash=${provider.privateKey.use { it.reader().readText() }.hashCode()}")
    }
}
