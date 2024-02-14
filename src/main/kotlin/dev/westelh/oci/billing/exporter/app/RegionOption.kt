package dev.westelh.oci.billing.exporter.app

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.oracle.bmc.Region

fun ParameterHolder.regionOption() = option().choice(*regionIds()).convert {
    Region.fromRegionId(it) ?: throw RuntimeException("Logic error: invalid region choice $it is suggested to user.")
}

private fun regionIds(): Array<String> {
    val regionList = Region.values()
    return Array(regionList.size) { index -> regionList[index].regionId }
}
