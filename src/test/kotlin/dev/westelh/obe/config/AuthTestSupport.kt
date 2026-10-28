package dev.westelh.obe.config

import java.io.File
import java.net.Proxy
import java.net.ProxySelector
import java.net.Socket
import java.net.SocketAddress
import java.net.URI
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

// Helpers shared by the offline tests of the real OCI SDK authentication providers.
// Nothing here talks to OCI or reads the developer's own ~/.oci directory.

fun generateRsaKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

/** PKCS#8 PEM ("BEGIN PRIVATE KEY") encoding of the private key. */
fun KeyPair.privateKeyPem(): String {
    val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(private.encoded)
    return "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----\n"
}

fun fileConfigOf(path: String, profile: String? = null): Config.AuthConfig.FileConfig {
    val yaml = buildString {
        appendLine("target: ocid1.tenancy.oc1..test")
        appendLine("auth:")
        appendLine("  config:")
        appendLine("    path: \"$path\"")
        if (profile != null) appendLine("    profile: \"$profile\"")
    }
    return Config.fromYaml(yaml.byteInputStream()).auth.config
}

/**
 * Makes any attempt to open a TCP connection (directly or through a proxy lookup) fail loudly.
 * Only meant to be installed in a throwaway child JVM, because it cannot be undone.
 */
@Suppress("DEPRECATION")
fun forbidNetworkAccess() {
    Socket.setSocketImplFactory { throw IllegalStateException("network access attempted") }
    ProxySelector.setDefault(object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> = throw IllegalStateException("network access attempted: $uri")
        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: java.io.IOException?) {}
    })
}

data class ChildJvmResult(val exitCode: Int, val stdout: String, val stderr: String) {
    /** Parses the `key=value` lines printed by a probe. */
    val values: Map<String, String> = stdout.lineSequence()
        .filter { '=' in it }
        .associate { it.substringBefore('=') to it.substringAfter('=') }
}

/**
 * Runs [mainClass] in a new JVM with the test classpath, an environment containing only [env],
 * and the given system properties. The empty environment keeps OCI_* variables of the developer
 * or CI machine (e.g. OCI_CONFIG_FILE, OCI_RESOURCE_PRINCIPAL_*) from leaking into the test.
 */
fun runInChildJvm(
    mainClass: KClass<*>,
    env: Map<String, String> = emptyMap(),
    systemProperties: Map<String, String> = emptyMap(),
): ChildJvmResult {
    val java = File(System.getProperty("java.home"), "bin/java").path
    val command = buildList {
        add(java)
        add("-cp")
        add(System.getProperty("java.class.path"))
        systemProperties.forEach { (key, value) -> add("-D$key=$value") }
        add(mainClass.java.name)
    }
    val stdout = File.createTempFile("child-jvm", ".out")
    val stderr = File.createTempFile("child-jvm", ".err")
    try {
        val process = ProcessBuilder(command)
            .redirectOutput(stdout)
            .redirectError(stderr)
            .apply { environment().clear(); environment().putAll(env) }
            .start()
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("child JVM timed out\n${stderr.readText()}")
        }
        return ChildJvmResult(process.exitValue(), stdout.readText(), stderr.readText())
    } finally {
        stdout.delete()
        stderr.delete()
    }
}
