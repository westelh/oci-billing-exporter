package dev.westelh.obe.config

import com.oracle.bmc.Region
import com.oracle.bmc.auth.AbstractFederationClientAuthenticationDetailsProviderBuilder
import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.io.path.createTempDirectory
import kotlin.system.exitProcess

// loadInstancePrincipalConfig talks to the instance metadata service (IMDS) and to the auth
// service's federation endpoint. Both are replaced by mock servers on the loopback address, and the
// provider runs in a child JVM that:
//  - has an empty environment, so OCI_METADATA_BASE_URL cannot redirect the SDK elsewhere,
//  - has an empty home directory, and
//  - refuses every connection to a non-loopback address (see LoopbackOnlySecurityManager), so even
//    a bug in the overrides cannot reach the real IMDS when the tests run on an OCI instance.
class InstancePrincipalsTest : StringSpec({
    "builds a provider from the certificates served by IMDS and a token from the federation endpoint" {
        MockOci().use { oci ->
            val result = runProbe(oci.metadataBaseUrl, oci.federationEndpoint, "token")

            withClue(result.toString()) {
                result.exitCode shouldBe 0
                result.values["keyId"] shouldBe "ST\$${oci.token}"
                result.values["hasPrivateKey"] shouldBe "true"

                val imdsPaths = oci.requests.filter { it.path.startsWith("/opc/v2/") }
                imdsPaths.map { it.path }.toSet() shouldContainAll setOf(
                    "/opc/v2/instance/region",
                    "/opc/v2/identity/cert.pem",
                    "/opc/v2/identity/key.pem",
                    "/opc/v2/identity/intermediate.pem",
                )
                imdsPaths.forEach { it.authorization shouldBe "Bearer Oracle" }

                val federation = oci.requests.filter { it.method == "POST" && it.path == "/v1/x509" }
                federation.shouldNotBeEmpty()
                // The request is signed with the leaf certificate, whose subject names the tenancy.
                federation.first().authorization shouldContain "keyId=\"$TENANT_ID/fed-x509"
                federation.first().body shouldContain base64(oci.identity.certificate.encoded)
            }
        }
    }

    "detects the region from IMDS when no federation endpoint is given" {
        MockOci().use { oci ->
            // Without a federation endpoint the SDK derives it from the region, so the probe stops
            // before anything would be sent to it (and the security manager would refuse it).
            val result = runProbe(oci.metadataBaseUrl, federationEndpoint = null, "region")

            withClue(result.toString()) {
                result.exitCode shouldBe 0
                result.values["region"] shouldBe Region.AP_TOKYO_1.regionId
                oci.requests.none { it.path.startsWith("/v1/") } shouldBe true
            }
        }
    }

    "gives up after retries + 1 attempts per lookup when IMDS does not respond" {
        for (retries in listOf(0, 1)) {
            ClosingServer().use { imds ->
                val result = runProbe(imds.baseUrl, federationEndpoint = null, "token", retries = retries)

                withClue("retries=$retries\n$result") {
                    result.exitCode shouldBe 1
                    // The SDK fetches the region twice (an IMDS v2 check whose failure is ignored,
                    // then the real lookup), each time trying 1 + retries times.
                    imds.connections.get() shouldBe 2 * (retries + 1)
                }
            }
        }
    }

    "passes the timeout to the SDK in milliseconds" {
        val config = instancePrincipalConfigOf(timeout = Duration.ofSeconds(10), retries = 3)

        val builder = instancePrincipalsBuilder(config)

        builder.protectedInt("timeoutForEachRetry") shouldBe 10_000
        builder.protectedInt("detectEndpointRetries") shouldBe 3
    }

    "waits for the configured timeout when IMDS does not accept connections" {
        UnresponsiveServer().use { imds ->
            val timeout = Duration.ofSeconds(3)

            val result = runProbe(imds.baseUrl, federationEndpoint = null, "token", timeout = timeout, retries = 0)

            withClue(result.toString()) {
                result.exitCode shouldBe 1
                result.values["error"] shouldContain "timed out"
                // Two lookups (see above) of one attempt each. The SDK also sleeps 1-2 s after each
                // failed attempt, so if the timeout were read as 3 ms the total would stay below 5 s.
                val elapsed = result.values.getValue("elapsedMillis").toLong()
                elapsed shouldBeGreaterThanOrEqual 2 * timeout.toMillis()
                elapsed shouldBeLessThan 2 * timeout.toMillis() + 10_000
            }
        }
    }

    "refuses to connect to the real IMDS address" {
        val result = runProbe(
            metadataBaseUrl = AbstractFederationClientAuthenticationDetailsProviderBuilder.METADATA_SERVICE_BASE_URL,
            federationEndpoint = null,
            "token",
            retries = 0,
        )

        withClue(result.toString()) {
            result.exitCode shouldBe 1
            result.values["error"] shouldContain "network access attempted: 169.254.169.254"
        }
    }
})

private const val TENANT_ID = "ocid1.tenancy.oc1..instanceprincipaltest"

private fun instancePrincipalConfigOf(timeout: Duration, retries: Int): Config.AuthConfig.InstancePrincipalConfig {
    val yaml = """
        target: ocid1.tenancy.oc1..test
        auth:
          instancePrincipal:
            timeout: "$timeout"
            retries: $retries
    """.trimIndent()
    return checkNotNull(Config.fromYaml(yaml.byteInputStream()).auth.instancePrincipal)
}

private fun Any.protectedInt(name: String): Int =
    AbstractFederationClientAuthenticationDetailsProviderBuilder::class.java.getDeclaredField(name)
        .apply { isAccessible = true }
        .getInt(this)

private fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

private fun pem(type: String, der: ByteArray): String {
    val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der)
    return "-----BEGIN $type-----\n$body\n-----END $type-----\n"
}

private fun runProbe(
    metadataBaseUrl: String,
    federationEndpoint: String?,
    mode: String,
    timeout: Duration = Duration.ofSeconds(2),
    retries: Int = 0,
): ChildJvmResult {
    val home = createTempDirectory("instance-principal-home").toFile()
    try {
        return runInChildJvm(
            InstancePrincipalProbe::class,
            home = home,
            args = listOf(mode, metadataBaseUrl, federationEndpoint ?: "-", timeout.toString(), retries.toString()),
            // Lets the probe install LoopbackOnlySecurityManager on Java 18 and later.
            jvmArgs = listOf("-Djava.security.manager=allow"),
        )
    } finally {
        home.deleteRecursively()
    }
}

/** A self-signed instance certificate and its key, generated with the JDK's keytool. */
private class InstanceIdentity(val certificate: X509Certificate, val privateKey: PrivateKey) {
    companion object {
        fun generate(dir: File): InstanceIdentity {
            val keystore = File(dir, "instance.p12")
            val password = "changeit"
            val keytool = File(System.getProperty("java.home"), "bin/keytool").path
            val process = ProcessBuilder(
                keytool, "-genkeypair", "-keyalg", "RSA", "-keysize", "2048", "-validity", "1",
                "-alias", "instance", "-storetype", "PKCS12", "-keystore", keystore.path,
                "-storepass", password, "-keypass", password,
                "-dname", "CN=ocid1.instance.oc1..instanceprincipaltest, OU=opc-tenant:$TENANT_ID",
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            check(process.waitFor(60, TimeUnit.SECONDS) && process.exitValue() == 0) { "keytool failed: $output" }

            val store = KeyStore.getInstance("PKCS12").apply {
                keystore.inputStream().use { load(it, password.toCharArray()) }
            }
            return InstanceIdentity(
                store.getCertificate("instance") as X509Certificate,
                store.getKey("instance", password.toCharArray()) as PrivateKey,
            )
        }
    }
}

/** IMDS and the federation endpoint on one loopback HTTP server. */
private class MockOci : AutoCloseable {
    data class Request(val method: String, val path: String, val authorization: String?, val body: String)

    private val identityDir = createTempDirectory("instance-principal-identity").toFile()
    val identity = InstanceIdentity.generate(identityDir)
    val token = unsignedJwt()
    val requests: MutableList<Request> = Collections.synchronizedList(mutableListOf())

    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
        val certPem = pem("CERTIFICATE", identity.certificate.encoded)
        val routes = mapOf(
            "/opc/v2/instance/region" to "ap-tokyo-1",
            "/opc/v2/identity/cert.pem" to certPem,
            "/opc/v2/identity/key.pem" to pem("PRIVATE KEY", identity.privateKey.encoded),
            "/opc/v2/identity/intermediate.pem" to certPem,
            "/v1/x509" to """{"token":"$token"}""",
        )
        createContext("/") { exchange ->
            exchange.use {
                val body = it.requestBody.readBytes().decodeToString()
                requests += Request(it.requestMethod, it.requestURI.path, it.requestHeaders.getFirst("Authorization"), body)
                val response = routes[it.requestURI.path]
                if (response == null) {
                    it.sendResponseHeaders(404, -1)
                } else {
                    val bytes = response.toByteArray()
                    it.responseHeaders.add("Content-Type", if (it.requestURI.path == "/v1/x509") "application/json" else "text/plain")
                    it.sendResponseHeaders(200, bytes.size.toLong())
                    it.responseBody.write(bytes)
                }
            }
        }
        start()
    }

    private val origin = "http://127.0.0.1:${server.address.port}"
    val metadataBaseUrl = "$origin/opc/v2/"
    val federationEndpoint = origin

    override fun close() {
        server.stop(0)
        identityDir.deleteRecursively()
    }

    private fun unsignedJwt(): String {
        val now = Instant.now().epochSecond
        val header = """{"alg":"none","typ":"JWT"}"""
        val payload = """{"sub":"ocid1.instance.oc1..instanceprincipaltest","tenant":"$TENANT_ID","iat":$now,"exp":${now + 3600}}"""
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf(header, payload, "unsigned").joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }
}

/** Accepts connections and closes them at once, counting how many arrived. */
private class ClosingServer : AutoCloseable {
    private val socket = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
    val connections = AtomicInteger()
    val baseUrl = "http://127.0.0.1:${socket.localPort}/opc/v2/"

    private val acceptor = thread(isDaemon = true) {
        while (!socket.isClosed) {
            runCatching { socket.accept() }.onSuccess { connections.incrementAndGet(); it.close() }
        }
    }

    override fun close() {
        socket.close()
        acceptor.join()
    }
}

/**
 * Never completes a TCP handshake: the server socket is never accepted from and its backlog is
 * filled, so the kernel drops further connection attempts and clients wait for their connect
 * timeout.
 */
private class UnresponsiveServer : AutoCloseable {
    private val socket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private val fillers = mutableListOf<Socket>()
    val baseUrl = "http://127.0.0.1:${socket.localPort}/opc/v2/"

    init {
        while (true) {
            val filler = Socket()
            try {
                filler.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), socket.localPort), 500)
                fillers += filler
            } catch (e: SocketTimeoutException) {
                filler.close()
                break
            }
            check(fillers.size < 100) { "could not fill the backlog of the server socket" }
        }
    }

    override fun close() {
        fillers.forEach { it.close() }
        socket.close()
    }
}

/** Refuses connections (and name lookups) for anything but the loopback address. */
@Suppress("DEPRECATION", "removal")
private class LoopbackOnlySecurityManager : SecurityManager() {
    override fun checkPermission(perm: java.security.Permission?) {}
    override fun checkPermission(perm: java.security.Permission?, context: Any?) {}

    override fun checkConnect(host: String, port: Int) = check(host)
    override fun checkConnect(host: String, port: Int, context: Any?) = check(host)

    private fun check(host: String) {
        val loopback = host == "localhost" || host.removePrefix("[").removeSuffix("]").let {
            it.startsWith("127.") || it == "::1" || it == "0:0:0:0:0:0:0:1"
        }
        if (!loopback) throw SecurityException("network access attempted: $host")
    }
}

/**
 * Entry point of the child JVM.
 * Arguments: mode (token|region), metadata base URL, federation endpoint or "-", timeout, retries.
 */
object InstancePrincipalProbe {
    @JvmStatic
    @Suppress("DEPRECATION", "removal")
    fun main(args: Array<String>) {
        System.setSecurityManager(LoopbackOnlySecurityManager())
        val (mode, metadataBaseUrl, federationEndpoint, timeout, retries) = args
        val start = System.nanoTime()
        try {
            val provider = loadInstancePrincipalConfig(
                instancePrincipalConfigOf(Duration.parse(timeout), retries.toInt()),
                metadataBaseUrl = metadataBaseUrl,
                federationEndpoint = federationEndpoint.takeUnless { it == "-" },
            )
            when (mode) {
                "region" -> println("region=${provider.region?.regionId}")
                "token" -> {
                    println("keyId=${provider.keyId}")
                    println("hasPrivateKey=${provider.privateKey.use { it.read() != -1 }}")
                }
            }
        } catch (e: Throwable) {
            println("elapsedMillis=${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)}")
            println("error=${e::class.java.name}: ${generateSequence(e) { it.cause }.joinToString(" <- ") { it.message?.lineSequence()?.first().orEmpty() }}")
            exitProcess(1)
        }
    }
}
