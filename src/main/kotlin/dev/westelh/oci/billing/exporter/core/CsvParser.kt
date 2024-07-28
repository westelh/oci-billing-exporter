package dev.westelh.oci.billing.exporter.core

import com.opencsv.bean.CsvToBeanBuilder
import com.opencsv.exceptions.CsvException
import java.io.InputStream
import java.io.InputStreamReader

class CsvParser : CostReportParser {
    override fun parse(inputStream: InputStream): CostReport? {
        val reader = InputStreamReader(inputStream)
        try {
            val parser = CsvToBeanBuilder<CostReport.Item>(reader).withType(CostReport.Item::class.java).build()
            return CostReport(parser.parse())
        } catch (e: CsvException) {
            return null
        }
    }
}