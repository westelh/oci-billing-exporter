package dev.westelh.oci.billing.exporter.core

import java.io.InputStream

interface BillingParser {
    fun parse(inputStream: InputStream): BillingReport
}