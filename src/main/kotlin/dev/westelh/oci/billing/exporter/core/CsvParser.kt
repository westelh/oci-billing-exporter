package dev.westelh.oci.billing.exporter.core

import com.opencsv.bean.CsvToBeanBuilder
import java.io.InputStream
import java.io.InputStreamReader

class CsvParser: BillingParser {
    override fun parse(inputStream: InputStream): BillingReport {
        val reader = InputStreamReader(inputStream)
        val parser = CsvToBeanBuilder<BilledItem>(reader).withType(BilledItem::class.java).build()
        return BillingReport(parser.parse())
    }
}