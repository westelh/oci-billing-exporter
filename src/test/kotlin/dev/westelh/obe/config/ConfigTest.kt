package dev.westelh.obe.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class ConfigTest : StringSpec({
    "minimal YAML keeps the documented defaults" {
        val config = Config.fromYaml("target: ocid1.tenancy.oc1..example".byteInputStream())

        config.targetTenantId shouldBe "ocid1.tenancy.oc1..example"
        config.server.port shouldBe 2112
        config.server.host.hostAddress shouldBe "127.0.0.1"
        config.server.delay shouldBe Duration.ofHours(6)
        config.server.download.maxRetries shouldBe 3
        config.server.download.partSizeInBytes shouldBe 4 * 1024 * 1024
        config.server.download.parallelDownloads shouldBe 5
        config.server.download.multipartDownloadThresholdInBytes shouldBe 4L * 1024 * 1024
        config.server.download.initialBackoff shouldBe Duration.ofSeconds(3)
        config.server.download.maxBackoff shouldBe Duration.ofDays(1)
        config.auth.config.profile shouldBe "DEFAULT"
    }

    "YAML overrides server, download, and authentication settings" {
        val config = Config.fromYaml("""
            target: ocid1.tenancy.oc1..target
            server:
              port: 8123
              host: 0.0.0.0
              delay: PT15M
              download:
                maxRetries: 7
                partSize: 8192
                parallelDownloads: 2
                multipartThreshold: 16384
                initialBackoff: PT1S
                maxBackoff: PT30S
            auth:
              config:
                path: /tmp/oci-config
                profile: BILLING
              instancePrincipal:
                timeout: PT20S
                retries: 4
              resourcePrincipal:
                timeout: PT25S
                retries: 5
        """.trimIndent().byteInputStream())

        config.server.port shouldBe 8123
        config.server.host.hostAddress shouldBe "0.0.0.0"
        config.server.delay shouldBe Duration.ofMinutes(15)
        config.server.download.maxRetries shouldBe 7
        config.server.download.partSizeInBytes shouldBe 8192
        config.server.download.parallelDownloads shouldBe 2
        config.server.download.multipartDownloadThresholdInBytes shouldBe 16384L
        config.server.download.initialBackoff shouldBe Duration.ofSeconds(1)
        config.server.download.maxBackoff shouldBe Duration.ofSeconds(30)
        config.auth.config.path shouldBe "/tmp/oci-config"
        config.auth.config.profile shouldBe "BILLING"
        config.auth.instancePrincipal?.timeout shouldBe Duration.ofSeconds(20)
        config.auth.instancePrincipal?.retries shouldBe 4
        config.auth.resourcePrincipal?.timeout shouldBe Duration.ofSeconds(25)
        config.auth.resourcePrincipal?.retries shouldBe 5
    }

    "invalid YAML and invalid duration values fail clearly" {
        shouldThrow<Exception> { Config.fromYaml("target: [".byteInputStream()) }
        shouldThrow<Exception> { Config.fromYaml("target: example\nserver:\n  delay: never".byteInputStream()) }
    }
})
