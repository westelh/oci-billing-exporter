package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import dev.westelh.oci.billing.exporter.api.ClientFactory

class InstancePrincipal : SubCommand(name = "instance", help = "Run server with instance principal") {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    override fun run() {
        kotlin.runCatching {
            InstancePrincipalsAuthenticationDetailsProvider.builder().build()
        }.onSuccess { auth ->
            val clientFactory = ClientFactory(auth)
            val client = clientFactory.createClient()
            Server(serverOptions, tenancy, client).run()
        }.onFailure {
            logger.atSevere().log("Authentication failed: %s", it.message)
            throw it
        }
    }
}