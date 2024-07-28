package dev.westelh.oci.billing.exporter.core

abstract class AbstractMetrics {
    abstract val name: String
    abstract val help: String

    /**
     * Map of label name and its content source expressed as a function return.
     */
    abstract val labelAndValueSources: Map<String, (CostReport.Item) -> String>

    fun labelNames(): List<String> = labelAndValueSources.keys.toList()

    fun getLabelValueOf(labelName: String, billedItem: CostReport.Item) = labelAndValueSources[labelName]?.invoke(billedItem)
}