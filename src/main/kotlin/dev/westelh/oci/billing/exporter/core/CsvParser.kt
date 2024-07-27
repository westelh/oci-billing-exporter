package dev.westelh.oci.billing.exporter.core

import com.opencsv.bean.CsvToBeanBuilder
import java.io.InputStream
import java.io.InputStreamReader

class CsvParser : CostReportParser {
    override fun parse(inputStream: InputStream): CostReport {
        val reader = InputStreamReader(inputStream)
        val parser = CsvToBeanBuilder<BilledItem>(reader).withType(BilledItem::class.java).build()
        return CostReport(parser.parse())
    }
}