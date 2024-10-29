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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.zip.GZIPInputStream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toKotlinDuration

class Run : CliktCommand() {
    private val config by option("--config").file(mustBeReadable = true, canBeDir = false).convert {
        Config.fromYaml(it)
    }.required()

    private val metrics = Metrics()

    companion object {
        val logger: FluentLogger = FluentLogger.forEnclosingClass()
    }

    override fun run() {
        JvmMetrics.builder().register()

        val httpServerLife = HTTPServerLife(buildConfiguration())
        addShutdownHook(httpServerLife)

        httpServerLife.use {
            runBlocking {
                loop(config.server.delay.toKotlinDuration()) {
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
            runCatching {
                loadFileConfig(config)
            }.recoverCatching {
                logger.atFiner().withCause(it).log("File config is invalid.")
                logger.atFine().log("Recovering authentication by instance principals.")
                loadInstancePrincipalConfig(
                    instancePrincipal ?: Config.AuthConfig.InstancePrincipalConfig()
                )
            }.recoverCatching {
                logger.atFiner().withCause(it).log("Instance principal is not available.")
                logger.atFine().log("Recovering authentication by resource principals.")
                loadResourcePrincipalConfig(
                    resourcePrincipal ?: Config.AuthConfig.ResourcePrincipalConfig()
                )
            }.getOrElse {
                logger.atFiner().withCause(it).log("Resource principal is not available.")
                throw RuntimeException("All of authentication method failed.")
            }
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

suspend fun <R> loop(delay: Duration, job: suspend () -> R) {
    val logger = FluentLogger.forEnclosingClass()
    while (true) {
        job()
        logger.atInfo().log("Sleeping for %s seconds...", delay.toString(DurationUnit.SECONDS))
        delay(delay)
    }
}
