package dev.westelh.obe

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.concurrent.suspension.shouldCompleteWithin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class LoopKtTest : StringSpec({
    "job is cancellable" {
        runBlocking {
            shouldCompleteWithin(100.milliseconds) {
                launch { repeatInfinite(Duration.INFINITE) { } }.cancelAndJoin()
            }
        }
    }

    "cancellation exception does not halt the code" {
        runBlocking {
            shouldNotThrowAny {
                launch { repeatInfinite(Duration.ZERO) { throw CancellationException() } }
            }
        }
    }
})
