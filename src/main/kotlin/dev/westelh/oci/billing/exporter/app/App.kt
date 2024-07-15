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
    val sessionToken by option().required()
    val tenantId by option()
    val userId by option()
    val privateKey by option().file(mustBeReadable = true, canBeDir = false)
    val fingerprint by option()
    val region by option().choice(*allRegions.codes()).convert { getRegionFromCode(it) }.required()
    private val impl = SessionTokenAuthenticationDetailsProvider.builder()
        .region(region)
        .tenantId(tenantId)
        .userId(userId)
        .sessionToken(sessionToken)
        .privateKeyFilePath(privateKey?.path)
        .fingerprint(fingerprint)
        .build()

    override fun makeClientSecretFromOptions(): ClientSecret = ClientSecret(impl)
}

abstract class ApiKeyBaseOptions(name: String) : AuthArguments(name) {
    val tenantId by option().required()
    val userId by option().required().check { it.isNotBlank() }
    val fingerprint by option().required().check { it.isNotBlank() }
    val passphrase by option()
    val passphraseCharacters by option()
    val region by option().choice(*allRegions.codes()).convert { getRegionFromCode(it) }.required()
}

class FromApiKey : ApiKeyBaseOptions("Options for authentication by api key") {
    val privateKey by option().required().check { it.isNotBlank() }
    val impl = SimpleAuthenticationDetailsProvider.builder()
        .region(region)
        .tenantId(tenantId)
        .userId(userId)
        .privateKeySupplier(StringPrivateKeySupplier(privateKey))
        .fingerprint(fingerprint)
        .passPhrase(passphrase)
        .passphraseCharacters(passphraseCharacters?.toCharArray())
        .build()

    override fun makeClientSecretFromOptions(): ClientSecret = ClientSecret(impl)
}

class FromApiKeyFile : ApiKeyBaseOptions("Options for authentication by api key file") {
    val privateKeyFile by option().file(mustBeReadable = true, canBeDir = false).required()
    private val impl = SimpleAuthenticationDetailsProvider.builder()
        .region(region)
        .tenantId(tenantId)
        .userId(userId)
        .privateKeySupplier(SimplePrivateKeySupplier(privateKeyFile.path))
        .passPhrase(passphrase)
        .passphraseCharacters(passphraseCharacters?.toCharArray())
        .build()

    override fun makeClientSecretFromOptions(): ClientSecret = ClientSecret(impl)
}

class App : CliktCommand(name = "oci_billing_exporter") {
    val target by option("--target", help = "Specify target tenancy to monitor by its id").required()

    // Authentication method choice (Required)
    val auth by option().groupChoice(
        "instance-principal" to FromInstancePrincipal(),
        "resource-principal" to FromResourcePrincipal(),
        "config" to FromConfigFile()
    ).required()

    // Options related to the web server (Optional)
    val serverOptions by ServerOptions()

    class ServerOptions : OptionGroup() {
        val interval by option(help = "Interval between refresh in milliseconds").long().default(21600000)
        val intervalOnError by option(help = "Interval between refresh when something went wrong").long().default(10000)
        val port by option(help = "Bind port").int().default(8080)
    }

    override fun run() {
        Server(serverOptions, target, auth).run()
    }
}

fun Array<Region>.codes(): Array<String> = map { it.regionCode }.toTypedArray()