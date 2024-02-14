package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.*
import com.google.common.flogger.FluentLogger
import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.ConfigFileReader.ConfigFile
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.auth.SimplePrivateKeySupplier
import com.oracle.bmc.auth.StringPrivateKeySupplier
import dev.westelh.oci.billing.exporter.api.ClientFactory
import dev.westelh.oci.billing.exporter.core.Server
import java.io.File

class ApiKey : SubCommand(name = "api_key", help = "Run server with api key") {
    class MiscellaneousOptions : OptionGroup() {
        val user by option().required()
        val fingerprint by option().required()
        val privateKey by option().check { it.isNotBlank() }
        val privateKeyFile by readableFileOption()
        val region by regionOption()
    }
    private val configFile: File? by readableFileOption()
    private val miscellaneousOptions by MiscellaneousOptions().cooccurring()

    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private fun loadConfigFile(): ConfigFile? {
        if (configFile != null) {
            logger.atInfo().log("Loading config file: %s", configFile)
            return try {
                ConfigFileReader.parse(configFile?.path)
            } catch (e: Exception) {
                logger.atWarning().log("Failed to load config file: %s", configFile)
                null
            }
        }
        else return null
    }

    private fun makeAuthenticationProviderFromConfigFile(configFile: ConfigFile): ConfigFileAuthenticationDetailsProvider? {
        logger.atInfo().log("Making credential object using config file: %s", configFile)
        try {
            val provider = ConfigFileAuthenticationDetailsProvider(configFile)
            logger.atInfo().log("Credential object is successfully created from config file")
            return provider
        } catch (e: Exception) {
            logger.atWarning().log("Failed to make credential object with exception: %s", e)
            return null
        }
    }

    private fun makeAuthenticationProviderWithMiscellaneousOptions(): SimpleAuthenticationDetailsProvider? {
        logger.atInfo().log("Making credential object using miscellaneous options: %s", miscellaneousOptions)
        return try {
            val privateKeySupplier = if (miscellaneousOptions?.privateKey != null) {
                StringPrivateKeySupplier(miscellaneousOptions?.privateKey)
            } else {
                SimplePrivateKeySupplier(miscellaneousOptions?.privateKeyFile?.path)
            }
            SimpleAuthenticationDetailsProvider.builder()
                .region(miscellaneousOptions?.region)
                .tenantId(tenancy)
                .userId(miscellaneousOptions?.user)
                .privateKeySupplier(privateKeySupplier)
                .fingerprint(miscellaneousOptions?.fingerprint)
                .build()
        } catch (e: Exception) {
            logger.atWarning().log("Failed to make credential object with exception: %s", e)
            null
        }
    }

    override fun run() {
        val configFile = loadConfigFile()
        val clientFactory = if (configFile != null) {
            val auth = makeAuthenticationProviderFromConfigFile(configFile)
            ClientFactory(auth!!)
        } else {
            ClientFactory(makeAuthenticationProviderWithMiscellaneousOptions()!!)
        }

        val client = clientFactory.createClient()

        Server(serverOptions, tenancy, client).run()
    }
}