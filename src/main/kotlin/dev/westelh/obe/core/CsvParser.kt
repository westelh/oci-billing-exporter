package dev.westelh.obe.core

import com.opencsv.bean.CsvToBeanBuilder
import com.opencsv.exceptions.CsvException
import java.io.InputStream
import java.io.InputStreamReader

class CsvParser : CostReportParser {
    override fun parse(inputStream: InputStream): CostReport {
        val reader = InputStreamReader(inputStream)
        val parser = CsvToBeanBuilder<CostReport.Item>(reader).withType(CostReport.Item::class.java).build()
        return CostReport(parser.parse())
    }
}