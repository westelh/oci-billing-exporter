package dev.westelh.oci.billing.exporter.core

abstract class AbstractMetrics {
    abstract val name: String
    abstract val help: String

    /**
     * Map of label name and its content source expressed as a function return.
     */
    abstract val labelAndValueSources: Map<String, (BilledItem) -> String>

    fun labelNames(): List<String> = labelAndValueSources.keys.toList()

    fun getLabelValueOf(labelName: String, billedItem: BilledItem) = labelAndValueSources[labelName]?.invoke(billedItem)
}