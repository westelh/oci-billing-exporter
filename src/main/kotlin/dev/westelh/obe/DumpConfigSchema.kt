package dev.westelh.obe

import com.github.ajalt.clikt.core.CliktCommand
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import dev.westelh.obe.config.Config

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