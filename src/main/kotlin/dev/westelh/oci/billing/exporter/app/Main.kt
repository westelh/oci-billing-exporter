package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import kotlinx.coroutines.runBlocking
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

class DumpConfigSchema : CliktCommand() {
    override fun run() {
        val configBuilder = SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON).with(
            JacksonModule()
        )
        val config = configBuilder.build()
        val generator = SchemaGenerator(config)
        val schema = generator.generateSchema(Config::class.java)
        print(schema)
    }
}

class Run : CliktCommand() {
    private val configFile by option("--config").file(mustBeReadable = true, canBeDir = false).required()

    override fun run() {
        val config = configFromYamlFile(configFile)
        val client = Client(config)
        val server = Server(config.server, client)
        Runtime.getRuntime().addShutdownHook(Thread {
            server.close()
        })
        runBlocking {
            server.run()
            server.join()
        }
    }
}

private fun configureLogManager() {
    val fileSpec: String? = System.getProperty("java.util.logging.config.file")
    val classSpec: String? = System.getProperty("java.util.logging.config.class")
    if (fileSpec.isNullOrBlank() && classSpec.isNullOrBlank()) {
        val bundled = Main::class.java.getResourceAsStream("/logging.properties")
        LogManager.getLogManager().readConfiguration(bundled)
    }
}