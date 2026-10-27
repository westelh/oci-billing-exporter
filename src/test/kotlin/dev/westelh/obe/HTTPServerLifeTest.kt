package dev.westelh.obe

import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.model.registry.PrometheusRegistry
import java.net.InetAddress
import java.net.URI
import java.net.ConnectException

class HTTPServerLifeTest : StringSpec({
    "serves metrics on a dynamic loopback port and stops on close" {
        val metrics = Metrics()
        val gauges = listOf(metrics.billedQuantity, metrics.cost, metrics.unitPrice)
        try {
            metrics.cost.labelValues("resource", "compute", "compartment", "region", "ad", "description").set(12.5)
            val server = HTTPServerLife(
                HTTPServer.builder().port(0).inetAddress(InetAddress.getLoopbackAddress())
            )
            val port = server.port
            try {
                val response = URI("http://127.0.0.1:$port/metrics").toURL().openStream()
                    .bufferedReader().use { it.readText() }
                response.contains("oci_billing_cost_charged").shouldBe(true)
                response.contains("12.5").shouldBe(true)
            } finally {
                server.close()
            }
            shouldThrow<ConnectException> {
                URI("http://127.0.0.1:$port/metrics").toURL().openConnection().apply { connectTimeout = 500 }
                    .getInputStream().close()
            }
        } finally {
            gauges.forEach { PrometheusRegistry.defaultRegistry.unregister(it) }
        }
    }
})
