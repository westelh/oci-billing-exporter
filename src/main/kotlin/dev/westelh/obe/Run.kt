package dev.westelh.obe

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.google.common.flogger.FluentLogger
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import dev.westelh.obe.config.*
import dev.westelh.obe.core.JacksonCsvParser
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import java.util.zip.GZIPInputStream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toKotlinDuration

class Run : CliktCommand() {
    private val config by option("--config").file(mustBeReadable = true, canBeDir = false).convert {
        Config.fromYaml(it)
    }.required()

    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    override fun run() {
        val metrics = Metrics()
        JvmMetrics.builder().register()

        val httpServerLife = HTTPServerLife(buildConfiguration())
        addShutdownHook(httpServerLife)

        httpServerLife.use {
            runBlocking {
                repeatInfinite(config.server.delay.toKotlinDuration()) {
                    val report = runCatching {
                        // To get SDK initialized eagerly, trying build them inside loop and try{}.
                        val auth = getAuthentication()
                        val downloader = CostReportDownloader(auth, config)

                        downloader.listAllCostReport()?.let { all ->
                            val latest = all.maxBy { it.name }
                            downloader.downloadCostReport(latest.name)?.let {
                                it.inputStream.use {
                                    val report = JacksonCsvParser().parse(GZIPInputStream(it))
                                    report.items.forEach { metrics.record(it) }
                                    report
                                }
                            }
                        }
                    }.getOrNull()

                    if (report != null) {
                        logger.atInfo().log("Synced %d items", report.items.size)
                    } else {
                        logger.atWarning().log("Failed to write metrics")
                    }
                }
            }
        }
    }

    private fun getAuthentication(): AbstractAuthenticationDetailsProvider {
        return with(config.auth) {
            loadAuthenticationWithFallback(
                file = { loadFileConfig(config) },
                instancePrincipal = {
                    loadInstancePrincipalConfig(instancePrincipal ?: Config.AuthConfig.InstancePrincipalConfig())
                },
                resourcePrincipal = {
                    loadResourcePrincipalConfig(resourcePrincipal ?: Config.AuthConfig.ResourcePrincipalConfig())
                },
                onFailure = { method, error ->
                    when (method) {
                        "File config" -> {
                            logger.atFiner().withCause(error).log("File config is invalid.")
                            logger.atFine().log("Recovering authentication by instance principals.")
                        }
                        "Instance principal" -> {
                            logger.atFiner().withCause(error).log("Instance principal is not available.")
                            logger.atFine().log("Recovering authentication by resource principals.")
                        }
                        "Resource principal" -> logger.atFiner().withCause(error).log("Resource principal is not available.")
                    }
                }
            )
        }
    }

    private fun addShutdownHook(httpServerLife: HTTPServerLife) {
        Runtime.getRuntime().addShutdownHook(Thread {
            httpServerLife.close()
        })
    }

    private fun buildConfiguration(): HTTPServer.Builder {
        with(config.server) {
            return HTTPServer.builder().port(port).inetAddress(host)
        }
    }
}

internal fun <T> loadAuthenticationWithFallback(
    file: () -> T,
    instancePrincipal: () -> T,
    resourcePrincipal: () -> T,
    onFailure: (String, Throwable) -> Unit = { _, _ -> }
): T {
    runCatching(file).onFailure { onFailure("File config", it) }.getOrNull()?.let { return it }
    runCatching(instancePrincipal).onFailure { onFailure("Instance principal", it) }.getOrNull()?.let { return it }
    return runCatching(resourcePrincipal).onFailure { onFailure("Resource principal", it) }
        .getOrElse { throw RuntimeException("All of authentication method failed.", it) }
}

suspend fun <R> repeatInfinite(delay: Duration, job: suspend CoroutineScope.() -> R) {
    val logger = FluentLogger.forEnclosingClass()
    while (true) {
        supervisorScope(job)
        logger.atInfo().log("Sleeping for %s seconds...", delay.toString(DurationUnit.SECONDS))
        delay(delay)
    }
}
