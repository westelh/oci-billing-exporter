package dev.westelh.obe.app

import dev.westelh.obe.loop
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.concurrent.suspension.shouldCompleteWithin
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration

class LoopKtTest {
    @Test
    fun jobIsCancellable() {
        runBlocking {
            shouldCompleteWithin(100, TimeUnit.MILLISECONDS) {
                launch { loop(Duration.INFINITE) { } }.cancelAndJoin()
            }
        }
    }

    @Test
    fun cancellationExceptionDoesNotHaltTheCode() {
        runBlocking {
            shouldNotThrowAny {
                launch { loop(Duration.ZERO) { throw CancellationException() } }
            }
        }
    }
}
