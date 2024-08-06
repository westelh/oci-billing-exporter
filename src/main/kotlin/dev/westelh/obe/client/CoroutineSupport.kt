package dev.westelh.obe.client

import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

// Suspend function by kotlinx.coroutine integration with java.util.concurrent.CompletionStage
// Suspends coroutine until Future.get() get returned which runs in common pool in ForkJoinPool
suspend fun<V> Future<V>.awaitInCommonPool(): V = CompletableFuture.supplyAsync(this::get).await()
