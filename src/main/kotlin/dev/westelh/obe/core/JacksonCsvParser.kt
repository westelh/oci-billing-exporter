package dev.westelh.obe.core

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.InputStream

class JacksonCsvParser : CostReportParser {
    private val mapper = CsvMapper()

    override fun parse(inputStream: InputStream): CostReport {
        val items = mapper
            .readerFor(CostReport.Item::class.java)
            .with(CsvSchema.emptySchema().withHeader())
            .readValues<CostReport.Item>(inputStream)
            .readAll()
        return CostReport(items)
    }
}
