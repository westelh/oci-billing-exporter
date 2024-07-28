package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.model.BmcException

// Common code to write logs for BmcExceptions
fun FluentLogger.logWithBmcException(e: BmcException) {
    atWarning().withCause(e).log("Calling API has failed with an exception")
    atFine().log("")
}