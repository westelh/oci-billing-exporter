package dev.westelh.oci.billing.exporter.api

import com.oracle.bmc.http.ClientConfigurator

object GlobalClientConfigurator {
    var configurator: ClientConfigurator? = null
}