package dev.westelh.oci.billing.exporter.core

import com.opencsv.bean.CsvBindByName

class BilledItem {
    @CsvBindByName(column = "lineItem/referenceNo")
    var referenceNo: String = ""

    @CsvBindByName(column = "lineItem/tenantId")
    var tenantId: String = ""

    @CsvBindByName(column = "lineItem/intervalUsageStart")
    var intervalUsageStart: String = ""

    @CsvBindByName(column = "lineItem/intervalUsageEnd")
    var intervalUsageEnd: String = ""

    @CsvBindByName(column = "product/service")
    var service: String = ""

    @CsvBindByName(column = "product/compartmentId")
    var compartmentId: String = ""

    @CsvBindByName(column = "product/compartmentName")
    var compartmentName: String = ""

    @CsvBindByName(column = "product/region")
    var region: String = ""

    @CsvBindByName(column = "product/availabilityDomain")
    var availabilityDomain: String = ""

    @CsvBindByName(column = "product/resourceId")
    var resourceId: String = ""

    @CsvBindByName(column = "usage/billedQuantity")
    var billedQuantity: Double = 0.0

    @CsvBindByName(column = "usage/billedQuantityOverage")
    var billedQuantityOverage: String = ""

    @CsvBindByName(column = "cost/subscriptionId")
    var subscriptionId: String = ""

    @CsvBindByName(column = "cost/productSku")
    var productSku: String = ""

    @CsvBindByName(column = "product/Description")
    var description: String = ""

    @CsvBindByName(column = "cost/unitPrice")
    var unitPrice: Double = 0.0

    @CsvBindByName(column = "cost/unitPriceOverage")
    var unitPriceOverage: String = ""

    @CsvBindByName(column = "cost/myCost")
    var myCost: Double = 0.0

    @CsvBindByName(column = "cost/myCostOverage")
    var myCostOverage: String = ""

    @CsvBindByName(column = "cost/currencyCode")
    var currencyCode: String = ""

    @CsvBindByName(column = "cost/billingUnitReadable")
    var billingUnitReadable: String = ""

    @CsvBindByName(column = "cost/skuUnitDescription")
    var skuUnitDescription: String = ""

    @CsvBindByName(column = "cost/overageFlag")
    var overageFlag: String = ""

    @CsvBindByName(column = "lineItem/isCorrection")
    var isCorrection: String = ""

    @CsvBindByName(column = "lineItem/backreferenceNo")
    var backreferenceNo: String = ""

    @CsvBindByName(column = "tags/OKEclusterName")
    var OKEclusterName: String = ""

    @CsvBindByName(column = "tags/Oracle-Tags.CreatedBy")
    var createdBy: String = ""

    @CsvBindByName(column = "tags/Oracle-Tags.CreatedOn")
    var createdOn: String = ""

    @CsvBindByName(column = "tags/orcl-cloud.free-tier-retained")
    var freeTierRetained: String = ""
}