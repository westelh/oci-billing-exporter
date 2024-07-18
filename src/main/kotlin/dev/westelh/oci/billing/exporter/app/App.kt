package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.Region
import com.oracle.bmc.auth.*
import dev.westelh.oci.billing.exporter.app.config.configFromYamlFile
import dev.westelh.oci.billing.exporter.client.allRegions
import dev.westelh.oci.billing.exporter.client.getRegionFromCode

// Common and abstract parameters that application need for authentication
// Inherits OptionGroup and has high level abstraction
sealed class AuthArguments(name: String) : OptionGroup(name) {
    abstract fun makeClientSecretFromOptions(): ClientSecret
}

class FromInstancePrincipal : AuthArguments("Options for authentication by instance principals") {
    override fun makeClientSecretFromOptions(): ClientSecret {
        val impl = InstancePrincipalsAuthenticationDetailsProvider.builder().build()
        return ClientSecret(impl)
    }
}

class FromResourcePrincipal : AuthArguments("Options for authentication by resource principals") {
    override fun makeClientSecretFromOptions(): ClientSecret {
        val impl = ResourcePrincipalAuthenticationDetailsProvider.builder().build()
        return ClientSecret(impl)
    }
}

class FromConfigFile : AuthArguments("Options for authentication by config files") {
    private val configFile by option().file(mustBeReadable = true, canBeDir = false).required()
    override fun makeClientSecretFromOptions(): ClientSecret {
        val read = ConfigFileReader.parse(configFile.path)
        val impl = ConfigFileAuthenticationDetailsProvider(read)
        return ClientSecret(impl)
    }
}

class FromSessionToken : AuthArguments("Options for authentication by session tokens") {
    private val sessionToken by option().required()
    private val tenantId by option()
    private val userId by option()
    private val privateKey by option().file(mustBeReadable = true, canBeDir = false)
    private val fingerprint by option()
    private val region by option().choice(*allRegions.codes()).convert { getRegionFromCode(it) }.required()

    override fun makeClientSecretFromOptions(): ClientSecret {
        val impl = SessionTokenAuthenticationDetailsProvider.builder()
            .region(region)
            .tenantId(tenantId)
            .userId(userId)
            .sessionToken(sessionToken)
            .privateKeyFilePath(privateKey?.path)
            .fingerprint(fingerprint)
            .build()
        return ClientSecret(impl)
    }
}

abstract class ApiKeyBaseOptions(name: String) : AuthArguments(name) {
    private val tenantId by option().required()
    private val userId by option().required().check { it.isNotBlank() }
    private val fingerprint by option().required().check { it.isNotBlank() }
    private val passphrase by option()
    private val passphraseCharacters by option()
    private val region by option().choice(*allRegions.codes()).convert { getRegionFromCode(it) }.required()
    fun makeCommonPart(): SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder = SimpleAuthenticationDetailsProvider.builder()
        .region(region)
        .tenantId(tenantId)
        .userId(userId)
        .fingerprint(fingerprint)
        .passPhrase(passphrase)
        .passphraseCharacters(passphraseCharacters?.toCharArray())
}

class FromApiKey : ApiKeyBaseOptions("Options for authentication by api key") {
    private val privateKey by option().required().check { it.isNotBlank() }

    override fun makeClientSecretFromOptions(): ClientSecret {
        val impl = makeCommonPart()
            .privateKeySupplier(StringPrivateKeySupplier(privateKey))
            .build()
        return ClientSecret(impl)
    }
}

class FromApiKeyFile : ApiKeyBaseOptions("Options for authentication by api key file") {
    private val privateKeyFile by option().file(mustBeReadable = true, canBeDir = false).required()

    override fun makeClientSecretFromOptions(): ClientSecret {
        val impl = makeCommonPart()
            .privateKeySupplier(SimplePrivateKeySupplier(privateKeyFile.path))
            .build()
        return ClientSecret(impl)
    }
}

class App : CliktCommand(name = "oci_billing_exporter") {
    private val target by option("--target", help = "Specify target tenancy to monitor by its id").required()

    // Authentication method choice (Required)
    private val auth by option().groupChoice(
        "instance-principal" to FromInstancePrincipal(),
        "resource-principal" to FromResourcePrincipal(),
        "config" to FromConfigFile(),
        "session-token" to FromSessionToken(),
        "api-key" to FromApiKey(),
        "api-key-file" to FromApiKeyFile()
    ).required()

    // Options related to the web server (Optional)
    private val serverOptions by ServerOptions()

    class ServerOptions : OptionGroup() {
        val interval by option(help = "Interval between refresh in milliseconds").long().default(21600000)
        val intervalOnError by option(help = "Interval between refresh when something went wrong").long().default(10000)
        val port by option(help = "Bind port").int().default(8080)
    }

    private val configFile by option("--config").file(mustBeReadable = true, canBeDir = false).required()
    private val config = configFromYamlFile(configFile)

    override fun run() {
        val server = Server(serverOptions, target, auth)
        setShutdownHook(server)
        server.run()
    }

    private fun setShutdownHook(server: Server) {
        val worker = Thread {
            server.shutdown()
        }
        Runtime.getRuntime().addShutdownHook(worker)
    }
}

fun Array<Region>.codes(): Array<String> = map { it.regionCode }.toTypedArray()