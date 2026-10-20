package dev.westelh.obe.core

import dev.westelh.obe.Metrics
import dev.westelh.obe.record
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.prometheus.metrics.config.EscapingScheme
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import java.io.ByteArrayOutputStream

class MetricsTest : StringSpec({
    val resource = this.javaClass.getResource("/reports_cost-extra-columns-large.csv")!!
    CsvColumnScenarios.cases.forEach { scenario ->
        "${scenario.name} should preserve Prometheus metric names, values, and labels" {
            val report = scenario.csv.byteInputStream().use { JacksonCsvParser().parse(it) }
            val samples = exportedSamples(report)

            samples.shouldHaveSize(3)
            samples.toSet() shouldBe expectedSamples(1)
        }
    }

    "all 1000 CSV rows should appear in Prometheus output without missing values" {
        val report = resource.openStream().use { JacksonCsvParser().parse(it) }
        val samples = exportedSamples(report)

        // Each distinct resource must produce usage, cost, and unit price samples.
        samples.shouldHaveSize(3000)
        samples.toSet() shouldBe (1..1000).flatMap { expectedSamples(it) }.toSet()
    }
})

private data class ExportedSample(val name: String, val labels: Map<String, String>, val value: Double)

// Expectations come from the synthetic fixture's formulas, independently of the parsed report.
private fun expectedSamples(row: Int): Set<ExportedSample> {
    val labels = mapOf(
        "id" to "ocid1.volume.oc1.ap-tokyo-1.synthetic${row.toString().padStart(6, '0')}",
        "service" to "BLOCK_STORAGE",
        "compartment" to "csv-regression-test",
        "region" to "ap-tokyo-1",
        "ad" to "TEST:AP-TOKYO-1-AD-1",
        "desc" to "Synthetic block volume for CSV column regression"
    )
    return setOf(
        ExportedSample("oci_billing_billed_quantity", labels, row + 0.25),
        ExportedSample("oci_billing_cost_charged", labels, (row + 0.25) * 0.5),
        ExportedSample("oci_billing_unit_price", labels, 0.5)
    )
}

private fun exportedSamples(report: CostReport): List<ExportedSample> {
    val metrics = Metrics()
    val gauges = listOf(metrics.billedQuantity, metrics.cost, metrics.unitPrice)
    try {
        val registry = PrometheusRegistry()
        gauges.forEach { registry.register(it) }
        report.items.forEach { metrics.record(it) }
        // Exercise the actual Prometheus text writer, not just the in-memory gauge values.
        val output = ByteArrayOutputStream()
        PrometheusTextFormatWriter.create().write(output, registry.scrape(), EscapingScheme.DEFAULT)

        // Compare labels by name so exposition label order does not affect the assertions.
        return output.toString(Charsets.UTF_8).lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val labels = Regex("""(\w+)="([^"]*)"""").findAll(line.substringAfter('{').substringBefore('}'))
                    .associate { it.groupValues[1] to it.groupValues[2] }
                ExportedSample(line.substringBefore('{'), labels, line.substringAfterLast(' ').toDouble())
            }.toList()
    } finally {
        // Metrics registers globally; clean up only the collectors created by this test.
        gauges.forEach { PrometheusRegistry.defaultRegistry.unregister(it) }
    }
}
