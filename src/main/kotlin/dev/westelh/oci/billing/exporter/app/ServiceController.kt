package dev.westelh.oci.billing.exporter.app

import com.google.common.flogger.FluentLogger
import dev.westelh.oci.billing.exporter.app.client.DefaultObjectStorageFactory
import dev.westelh.oci.billing.exporter.app.client.Service
import dev.westelh.oci.billing.exporter.app.client.ServiceImpl

class ServiceController(private val authArguments: AuthArguments) {
    companion object

    val logger: FluentLogger = FluentLogger.forEnclosingClass()

    private var service: Service? = null

    private fun initService(): Service? {
        val clientSecret = try {
            authArguments.makeClientSecretFromOptions()
        } catch (e: Exception) {
            logger.atWarning().log("Failed to initialize SDK with provided auth options", e)
            return null
        }
        return try {
            val objectStorage = DefaultObjectStorageFactory(clientSecret).createObjectStorage()
            ServiceImpl(objectStorage)
        } catch (e: Exception) {
            logger.atWarning().log("Failed to initialize client service", e)
            return null
        }
    }

    fun getService(): Service? {
        if (service == null) service = initService()
        return service
    }
}