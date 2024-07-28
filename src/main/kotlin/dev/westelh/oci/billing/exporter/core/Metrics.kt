package dev.westelh.oci.billing.exporter.core

import io.prometheus.metrics.core.datapoints.GaugeDataPoint
import io.prometheus.metrics.core.metrics.Gauge

class Metrics {
    private val labels: Array<String> = arrayOf("id", "service", "compartment", "region", "ad", "desc")

    val billedQuantity: Gauge = Gauge.builder()
        .name("oci_billing_billed_quantity")
        .help("The quantity of the resource that has been billed over the usage interval")
        .labelNames(*labels)
        .register()

    val cost: Gauge = Gauge.builder()
        .name("oci_billing_cost_charged")
        .help("The cost charged for usage")
        .labelNames(*labels)
        .register()

    val unitPrice: Gauge = Gauge.builder()
        .name("oci_billing_unit_price")
        .help("The cost billed for each unit of the resource used")
        .labelNames(*labels)
        .register()
}

fun Metrics.record(item: CostReport.Item) {
    cost.labelBy(item).set(item.myCost)
    billedQuantity.labelBy(item).set(item.billedQuantity)
    unitPrice.labelBy(item).set(item.unitPrice)
}

private fun Gauge.labelBy(report: CostReport.Item): GaugeDataPoint {
    return labelValues(
        report.resourceId,
        report.service,
        report.compartmentName,
        report.region,
        report.availabilityDomain,
        report.description
    )
}