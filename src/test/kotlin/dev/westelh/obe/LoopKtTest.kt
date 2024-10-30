package dev.westelh.obe

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.concurrent.suspension.shouldCompleteWithin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration

class LoopKtTest {
    @Test
    fun jobIsCancellable() {
        runBlocking {
            shouldCompleteWithin(100, TimeUnit.MILLISECONDS) {
                launch { repeatInfinite(Duration.INFINITE) { } }.cancelAndJoin()
            }
        }
    }

    @Test
    fun cancellationExceptionDoesNotHaltTheCode() {
        runBlocking {
            shouldNotThrowAny {
                launch { repeatInfinite(Duration.ZERO) { throw CancellationException() } }
            }
        }
    }
}
