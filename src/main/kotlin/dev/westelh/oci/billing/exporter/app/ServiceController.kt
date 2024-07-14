package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import dev.westelh.oci.billing.exporter.client.DefaultObjectStorageFactory
import dev.westelh.oci.billing.exporter.client.CostReportService
import dev.westelh.oci.billing.exporter.client.SingleTenantCostReportService

class ServiceController(private val authArguments: AuthArguments, private val targetTenantId: String) {
    companion object

    val logger: FluentLogger = FluentLogger.forEnclosingClass()

    private var service: CostReportService? = null

    private fun initClientSecret(): ClientSecret? {
        return try {
            authArguments.makeClientSecretFromOptions()
        } catch (e: Exception) {
            logger.atWarning().log("Failed to initialize SDK with provided auth options with an exception: %s", e)
            return null
        }
    }

    private fun initService(): CostReportService? {
        val clientSecret = initClientSecret()
        if (clientSecret == null) return null
        else {
            return try {
                SingleTenantCostReportService(targetTenantId, DefaultObjectStorageFactory(clientSecret))
            } catch (e: Exception) {
                logger.atWarning().log("Failed to initialize client service with an exception: %s", e)
                return null
            }
        }
    }

    fun getService(): CostReportService? {
        if (service == null) service = initService()
        return service
    }
}

fun<T> ServiceController.whenServiceAvailable(runner: CostReportService.()->T): T? = getService()?.run(runner)