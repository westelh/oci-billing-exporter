package dev.westelh.oci.billing.exporter.app

import io.kotest.matchers.concurrent.suspension.shouldCompleteWithin
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration

class LoopKtTest {
    @Test
    fun cancellable() {
        runBlocking {
            shouldCompleteWithin(100, TimeUnit.MILLISECONDS) {
                launch { loop(Duration.INFINITE) { } }.cancelAndJoin()
            }
        }
    }
}
