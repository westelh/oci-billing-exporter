package dev.westelh.obe.core

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.InputStream

class JacksonCsvParser : CostReportParser {
    private val mapper = CsvMapper()

    override fun parse(inputStream: InputStream): CostReport {
        val iterator: MappingIterator<CostReport.Item> =
            mapper.readerFor(CostReport.Item::class.java).with(CostReport.Item.schema).readValues(inputStream)
        return CostReport(iterator.readAll())
    }
}