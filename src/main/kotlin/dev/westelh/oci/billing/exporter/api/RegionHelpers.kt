package dev.westelh.oci.billing.exporter.api

import com.oracle.bmc.Region

// All known regions contained by SDK
val allRegions: Array<Region> = Region.values()

// Converter functions
fun getRegionFromCode(regionCode: String): Region = Region.fromRegionCode(regionCode)
fun getRegionFromId(regionId: String): Region = Region.fromRegionId(regionId)