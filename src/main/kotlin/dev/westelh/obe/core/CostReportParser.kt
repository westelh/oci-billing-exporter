package dev.westelh.obe.core

import java.io.InputStream

interface CostReportParser {
    fun parse(inputStream: InputStream): CostReport?
}