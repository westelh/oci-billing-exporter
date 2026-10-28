package dev.westelh.obe.config

import com.oracle.bmc.Region
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.io.File
import java.security.MessageDigest
import kotlin.system.exitProcess

// loadFileConfig runs in a child JVM whose home directory is a temporary directory, because the SDK
// looks under ~/.oci even when an explicit path is given (every region lookup checks
// ~/.oci/developer-tool-configuration.json).
class FileConfigTest : StringSpec({
    "uses the DEFAULT profile of the config file at the given path" {
        val fixture = OciConfigFixture.write(tempdir())

        val result = runFileConfigProbe(tempdir(), fixture.config.path)

        withClue(result.toString()) {
            result.exitCode shouldBe 0
            result.values["userId"] shouldBe "ocid1.user.oc1..default"
            result.values["tenantId"] shouldBe "ocid1.tenancy.oc1..default"
            result.values["fingerprint"] shouldBe DEFAULT_FINGERPRINT
            result.values["region"] shouldBe Region.AP_TOKYO_1.regionId
            result.values["privateKeySha256"] shouldBe sha256(fixture.privateKeyPem)
        }
    }

    "uses the requested profile of the config file" {
        val fixture = OciConfigFixture.write(tempdir())

        val result = runFileConfigProbe(tempdir(), fixture.config.path, profile = "OTHER")

        withClue(result.toString()) {
            result.exitCode shouldBe 0
            result.values["userId"] shouldBe "ocid1.user.oc1..other"
            result.values["tenantId"] shouldBe "ocid1.tenancy.oc1..other"
            result.values["fingerprint"] shouldBe OTHER_FINGERPRINT
            result.values["region"] shouldBe Region.US_ASHBURN_1.regionId
            result.values["privateKeySha256"] shouldBe sha256(fixture.privateKeyPem)
        }
    }

    "fails when the profile does not exist" {
        val fixture = OciConfigFixture.write(tempdir())

        val result = runFileConfigProbe(tempdir(), fixture.config.path, profile = "MISSING")

        withClue(result.toString()) {
            result.exitCode shouldBe 1
            result.values["error"] shouldStartWith "java.lang.IllegalArgumentException:"
            result.values["error"] shouldContain "MISSING"
        }
    }

    "fails when the config file does not exist" {
        val missing = File(tempdir(), "no-such-config")

        val result = runFileConfigProbe(tempdir(), missing.path)

        withClue(result.toString()) {
            result.exitCode shouldBe 1
            result.values["error"] shouldStartWith "java.io.FileNotFoundException:"
        }
    }

    "fails on creation when the profile has no key_file entry" {
        val fixture = OciConfigFixture.write(tempdir(), includeKeyFile = false)

        val result = runFileConfigProbe(tempdir(), fixture.config.path)

        withClue(result.toString()) {
            result.exitCode shouldBe 1
            result.values["error"] shouldStartWith "java.lang.NullPointerException:"
            result.values["error"] shouldContain "key_file"
        }
    }

    "fails only when the private key is read if key_file points to a missing file" {
        val dir = tempdir()
        val fixture = OciConfigFixture.write(dir, keyFile = File(dir, "missing.pem").path)

        val result = runFileConfigProbe(tempdir(), fixture.config.path)

        // The SDK reads key_file lazily, so creating the provider still succeeds.
        withClue(result.toString()) {
            result.exitCode shouldBe 0
            result.values["userId"] shouldBe "ocid1.user.oc1..default"
            result.values["privateKeyError"] shouldStartWith "java.lang.IllegalArgumentException:"
            result.values["privateKeyError"] shouldContain "missing.pem"
        }
    }

    "reads ~/.oci/config when no path is given" {
        val home = tempdir()
        val ociDir = File(home, ".oci").apply { mkdirs() }
        val fixture = OciConfigFixture.write(ociDir, keyFile = "~/.oci/key.pem")

        val result = runFileConfigProbe(home, path = "")

        withClue(result.toString()) {
            result.exitCode shouldBe 0
            result.values["userId"] shouldBe "ocid1.user.oc1..default"
            result.values["tenantId"] shouldBe "ocid1.tenancy.oc1..default"
            result.values["region"] shouldBe Region.AP_TOKYO_1.regionId
            result.values["privateKeySha256"] shouldBe sha256(fixture.privateKeyPem)
        }
    }
})

private const val DEFAULT_FINGERPRINT = "11:11:11:11:11:11:11:11:11:11:11:11:11:11:11:11"
private const val OTHER_FINGERPRINT = "22:22:22:22:22:22:22:22:22:22:22:22:22:22:22:22"

private fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

/** Runs [FileConfigProbe] with an empty environment and [home] as the home directory. */
private fun runFileConfigProbe(home: File, path: String, profile: String = "DEFAULT") =
    runInChildJvm(FileConfigProbe::class, home = home, args = listOf(path, profile))

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

/**
 * Entry point of the child JVM. Arguments: the config path (empty for the default ~/.oci/config branch)
 * and the profile. Prints what the provider returned, or the error raised while creating it.
 */
object FileConfigProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        forbidNetworkAccess()
        val (path, profile) = args
        val provider = try {
            loadFileConfig(fileConfigOf(path, profile))
        } catch (e: Throwable) {
            println("error=${e.describe()}")
            exitProcess(1)
        }
        println("userId=${provider.userId}")
        println("tenantId=${provider.tenantId}")
        println("fingerprint=${provider.fingerprint}")
        println("region=${provider.region.regionId}")
        try {
            println("privateKeySha256=${sha256(provider.privateKey.use { it.reader().readText() })}")
        } catch (e: Throwable) {
            println("privateKeyError=${e.describe()}")
        }
    }

    private fun Throwable.describe() = "${this::class.java.name}: ${message?.lineSequence()?.first()}"
}
