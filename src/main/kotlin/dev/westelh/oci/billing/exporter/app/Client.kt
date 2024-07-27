package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.responses.AsyncHandler
import dev.westelh.oci.billing.exporter.client.CachedObjectStorage
import dev.westelh.oci.billing.exporter.client.await
import kotlinx.coroutines.*

class Client(adp: AuthenticationDetailsProvider, targetTenantId: String) {
    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private val requestFactory: RequestFactory = SimpleRequestFactory(targetTenantId)
    private val osFactory = CachedObjectStorage(adp)
    private val coroutineScope = CoroutineScope(Job())
    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        logger.atWarning().withCause(t).log("Unhandled exception from client code inside a coroutine")
    }

    suspend fun listAllCostReport(): Deferred<List<ObjectSummary>?> = coroutineScope.async(exceptionHandler) {
        val ret = mutableListOf<ObjectSummary>()
        var nextStartWith = ""
        // Enumeration
        do {
            val request = requestFactory.buildListCostReportsRequest(nextStartWith)
            val response = kotlin.runCatching {
                osFactory.getObjectStorageAsync().listObjects(request, CustomAsyncHandler()).await()
            }.getOrElse { return@async null /* Fast-return on error */}
            ret.addAll(response.listObjects.objects)
            nextStartWith = response.listObjects.nextStartWith ?: ""
        } while (nextStartWith.isNotBlank())
        ret
    }

    // suspend fun listUsageReport():

    class CustomAsyncHandler<Req, Res> : AsyncHandler<Req, Res> {
        override fun onSuccess(req: Req, res: Res) {
            logger.atFine().log("Client completed an async call, that is: %s", req)
        }

        override fun onError(req: Req, t: Throwable?) {
            logger.atWarning().withCause(t).log("Client failed invoking async call to: %s", req)
        }
    }
}
