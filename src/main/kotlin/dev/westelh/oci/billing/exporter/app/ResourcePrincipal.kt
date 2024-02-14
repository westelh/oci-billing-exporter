package dev.westelh.oci.billing.exporter.app

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider
import dev.westelh.oci.billing.exporter.api.ClientFactory
import dev.westelh.oci.billing.exporter.core.Server

class ResourcePrincipal : SubCommand(name = "resource", help = "Run server with resource principal") {
    override fun run() {
        val authProvider = ResourcePrincipalAuthenticationDetailsProvider.builder().build()
        val client = ClientFactory(authProvider).createClient()
        Server(options = serverOptions, tenancy = tenancy, client = client)
    }
}