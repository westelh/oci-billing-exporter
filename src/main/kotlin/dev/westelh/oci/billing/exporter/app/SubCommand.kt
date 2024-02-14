package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long

abstract class SubCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
    val serverOptions by ServerOptions()
    val tenancy by option().required()

    class ServerOptions : OptionGroup() {
        val interval by option(help = "Interval between refresh in milliseconds").long().default(21600000)
        val port by option(help = "Bind port").int().default(8080)
    }
}