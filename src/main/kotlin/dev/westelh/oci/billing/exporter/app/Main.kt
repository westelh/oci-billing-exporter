package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.core.subcommands


class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) = App().subcommands(
            ApiKey(),
            TokenCommand(),
            InstancePrincipal(),
            ResourcePrincipal()
        ).main(args)
    }
}