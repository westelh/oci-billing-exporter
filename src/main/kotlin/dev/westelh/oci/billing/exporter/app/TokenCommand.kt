package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider
import dev.westelh.oci.billing.exporter.api.ClientFactory
import dev.westelh.oci.billing.exporter.core.Server
import java.io.File

class TokenCommand : SubCommand(name = "token", help = "Run server with session token") {
    private val region by regionOption().required()
    private val privateKeyFile by option(help = "Path to private key file").file(mustExist = true, mustBeReadable = true, canBeDir = false).required()
    private val token by mutuallyExclusiveOptions(
        option("--token").convert { TokenString(it) },
        option("--token-path").file(mustExist = true, mustBeReadable = true, canBeDir = false).convert { TokenFile(it) }
    ).required()

    override fun run() {
        val provider = SessionTokenAuthenticationDetailsProvider.builder()
            .tenantId(tenancy)
            .region(region)
            .privateKeyFilePath(privateKeyFile.path)
            .also {
                when(val immutableToken = token) {
                    is TokenString -> it.sessionToken(immutableToken.token)
                    is TokenFile -> it.sessionTokenFilePath(immutableToken.file.path)
                }
            }.build()
        val client = ClientFactory(provider).createClient()
        val thread = Thread(Server(serverOptions, tenancy, client))
        thread.start()
        thread.join()
        provider.close()
    }
}

sealed class Token

class TokenString(val token: String) : Token()

class TokenFile(val file: File) : Token()