package dev.westelh.oci.billing.exporter.core

import java.io.InputStream

interface CostReportParser {
    fun parse(inputStream: InputStream): CostReport?
}