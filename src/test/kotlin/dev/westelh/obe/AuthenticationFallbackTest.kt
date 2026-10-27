package dev.westelh.obe

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AuthenticationFallbackTest : StringSpec({
    "uses file configuration when the first authentication method succeeds" {
        val attempts = mutableListOf<String>()
        val selected = loadAuthenticationWithFallback(
            file = { attempts += "file"; "file-provider" },
            instancePrincipal = { attempts += "instance"; error("must not be attempted") },
            resourcePrincipal = { attempts += "resource"; error("must not be attempted") }
        )

        selected shouldBe "file-provider"
        attempts shouldBe listOf("file")
    }

    "falls back from file config to instance principal and then resource principal" {
        val attempts = mutableListOf<String>()
        val selected = loadAuthenticationWithFallback(
            file = { attempts += "file"; error("no config file") },
            instancePrincipal = { attempts += "instance"; error("no instance identity") },
            resourcePrincipal = { attempts += "resource"; "resource-provider" }
        )

        selected shouldBe "resource-provider"
        attempts shouldBe listOf("file", "instance", "resource")
    }

    "reports failure when every authentication method fails" {
        val error = shouldThrow<RuntimeException> {
            loadAuthenticationWithFallback(
                file = { error("file") },
                instancePrincipal = { error("instance") },
                resourcePrincipal = { error("resource") }
            )
        }
        error.message shouldBe "All of authentication method failed."
        error.cause?.message shouldBe "resource"
    }
})
