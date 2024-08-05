package dev.westelh.obe

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import java.util.logging.LogManager

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            configureLogManager()
            App().subcommands(Run(), DumpConfigSchema()).main(args)
        }
    }
}

class App : CliktCommand(name = "oci-billing-exporter") {
    override fun run() = Unit
}

private fun configureLogManager() {
    val fileSpec: String? = System.getProperty("java.util.logging.config.file")
    val classSpec: String? = System.getProperty("java.util.logging.config.class")
    if (fileSpec.isNullOrBlank() && classSpec.isNullOrBlank()) {
        val bundled = Main::class.java.getResourceAsStream("/logging.properties")
        LogManager.getLogManager().readConfiguration(bundled)
    }
}

