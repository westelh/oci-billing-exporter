package dev.westelh.oci.billing.exporter.app

import kotlinx.coroutines.delay
import kotlin.time.Duration

suspend fun<R> loop(delay: Duration, job: suspend () -> R) {
    while (true) {
        job()
        delay(delay)
    }
}