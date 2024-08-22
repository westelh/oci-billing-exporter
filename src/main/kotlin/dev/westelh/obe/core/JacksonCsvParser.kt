package dev.westelh.obe.core

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvParser.Feature
import com.fasterxml.jackson.dataformat.csv.CsvReadException
import com.google.common.flogger.FluentLogger
import java.io.InputStream

class JacksonCsvParser : CostReportParser {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private val mapper = CsvMapper()

    override fun parse(inputStream: InputStream): CostReport {
        // Try first with default reader
        val list = kotlin.runCatching {
            defaultReader().readValues<CostReport.Item>(inputStream).readAll()
        }.recover {
            if (it is CsvReadException) {
                // Recovery with eased reader
                logger.atWarning().withCause(it).log("Recovering from failure by default csv reader.")
                easedReader().readValues<CostReport.Item>(inputStream).readAll()
            }
            else throw it
        }.getOrThrow()
        return CostReport(list)
    }

    private fun defaultReader(): ObjectReader {
        return mapper
            .readerFor(CostReport.Item::class.java)
            .with(CostReport.Item.schema)
    }

    private fun easedReader(): ObjectReader {
        return mapper
            .enable(Feature.IGNORE_TRAILING_UNMAPPABLE)
            .readerFor(CostReport.Item::class.java)
            .with(CostReport.Item.schema)
    }
}