package dev.westelh.obe.core

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.dataformat.csv.CsvMapper

data class CostReport(val items: List<Item>) {

    @JsonPropertyOrder(
        "lineItem/referenceNo",
        "lineItem/tenantId",
        "lineItem/intervalUsageStart",
        "lineItem/intervalUsageEnd",
        "product/service",
        "product/compartmentId",
        "product/compartmentName",
        "product/region",
        "product/availabilityDomain",
        "product/resourceId",
        "usage/billedQuantity",
        "usage/billedQuantityOverage",
        "cost/subscriptionId",
        "cost/productSku",
        "product/Description",
        "cost/unitPrice",
        "cost/unitPriceOverage",
        "cost/myCost",
        "cost/myCostOverage",
        "cost/currencyCode",
        "cost/billingUnitReadable",
        "cost/skuUnitDescription",
        "cost/overageFlag",
        "lineItem/isCorrection",
        "lineItem/backreferenceNo",
        "tags/OKEclusterName",
        "tags/Oracle-Tags.CreatedBy",
        "tags/Oracle-Tags.CreatedOn",
        "tags/orcl-cloud.free-tier-retained"
    )
    class Item {
        @JsonProperty("lineItem/referenceNo")
        var referenceNo: String = ""

        @JsonProperty("lineItem/tenantId")
        var tenantId: String = ""

        @JsonProperty("lineItem/intervalUsageStart")
        var intervalUsageStart: String = ""

        @JsonProperty("lineItem/intervalUsageEnd")
        var intervalUsageEnd: String = ""

        @JsonProperty("product/service")
        var service: String = ""

        @JsonProperty("product/compartmentId")
        var compartmentId: String = ""

        @JsonProperty("product/compartmentName")
        var compartmentName: String = ""

        @JsonProperty("product/region")
        var region: String = ""

        @JsonProperty("product/availabilityDomain")
        var availabilityDomain: String = ""

        @JsonProperty("product/resourceId")
        var resourceId: String = ""

        @JsonProperty("usage/billedQuantity")
        var billedQuantity: Double = 0.0

        @JsonProperty("usage/billedQuantityOverage")
        var billedQuantityOverage: String = ""

        @JsonProperty("cost/subscriptionId")
        var subscriptionId: String = ""

        @JsonProperty("cost/productSku")
        var productSku: String = ""

        @JsonProperty("product/Description")
        var description: String = ""

        @JsonProperty("cost/unitPrice")
        var unitPrice: Double = 0.0

        @JsonProperty("cost/unitPriceOverage")
        var unitPriceOverage: String = ""

        @JsonProperty("cost/myCost")
        var myCost: Double = 0.0

        @JsonProperty("cost/myCostOverage")
        var myCostOverage: String = ""

        @JsonProperty("cost/currencyCode")
        var currencyCode: String = ""

        @JsonProperty("cost/billingUnitReadable")
        var billingUnitReadable: String = ""

        @JsonProperty("cost/skuUnitDescription")
        var skuUnitDescription: String = ""

        @JsonProperty("cost/overageFlag")
        var overageFlag: String = ""

        @JsonProperty("lineItem/isCorrection")
        var isCorrection: String = ""

        @JsonProperty("lineItem/backreferenceNo")
        var backreferenceNo: String = ""

        @JsonProperty("tags/OKEclusterName")
        var OKEclusterName: String = ""

        @JsonProperty("tags/Oracle-Tags.CreatedBy")
        var createdBy: String = ""

        @JsonProperty("tags/Oracle-Tags.CreatedOn")
        var createdOn: String = ""

        @JsonProperty("tags/orcl-cloud.free-tier-retained")
        var freeTierRetained: String = ""

        companion object {
            val schema = CsvMapper().schemaFor(Item::class.java).withHeader()
        }
    }
}

