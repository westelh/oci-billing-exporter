package dev.westelh.obe

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.context
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CliTest : StringSpec({
    "root and run help render the expected command and required option" {
        val rootHelp = captureStdout { Main.createCli().context { exitProcess = {} }.main(arrayOf("--help")) }
        rootHelp.contains("run").shouldBe(true)
        rootHelp.contains("dump-config-schema").shouldBe(true)

        val runHelp = captureStdout { Main.createCli().context { exitProcess = {} }.main(arrayOf("run", "--help")) }
        runHelp.contains("--config").shouldBe(true)
    }

    "dump-config-schema emits valid JSON with the target and server properties" {
        val json = captureStdout { Main.createCli().context { exitProcess = {} }.main(arrayOf("dump-config-schema")) }
        val schema = ObjectMapper().readTree(json)

        schema.get("type").asText() shouldBe "object"
        val properties = schema.get("properties")
        properties.fieldNames().asSequence().toList() shouldContain "target"
        properties.fieldNames().asSequence().toList() shouldContain "server"
        schema.get("required").map { it.asText() } shouldContain "target"
    }

    "run rejects a missing or unreadable config with a usage error" {
        fun exitCode(args: Array<String>): Int {
            var code = -1
            captureStdout {
                try {
                    Main.createCli().context { exitProcess = { status -> throw CliExit(status) } }.main(args)
                } catch (exit: CliExit) {
                    code = exit.status
                }
            }
            return code
        }

        exitCode(arrayOf("run")) shouldBe 1
        exitCode(arrayOf("run", "--config", File("/path/that/does/not/exist").path)) shouldBe 1
    }
})

private class CliExit(val status: Int) : RuntimeException()

private fun captureStdout(block: () -> Unit): String {
    val original = System.out
    val bytes = ByteArrayOutputStream()
    System.setOut(PrintStream(bytes))
    try {
        block()
    } finally {
        System.setOut(original)
    }
    return bytes.toString(Charsets.UTF_8)
}
