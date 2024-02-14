package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

fun ParameterHolder.readableFileOption(help: String = ""): NullableOption<File, File> = option(help=help).file(
    mustExist = true,
    mustBeReadable = true,
    canBeDir = false
)