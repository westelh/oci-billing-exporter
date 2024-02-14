package dev.westelh

import dev.westelh.oci.billing.exporter.core.CsvParser
import org.junit.jupiter.api.assertDoesNotThrow
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CsvParserTest {
    private val testResourceName: String = "/reports_cost-csv_00000000012345-00000.csv"
    private val testResource: URL =
        this::class.java.getResource(testResourceName) ?: throw RuntimeException("Test resource $testResourceName not found")

    @Test
    fun testParsing() {
        val parser = CsvParser()
        testResource.openStream().use {
            val report = assertDoesNotThrow { parser.parse(it) }
            for (item in report.items) {
                with(item) {
                    referenceNo.assertNotEmpty()
                    tenantId.assertNotEmpty()
                    intervalUsageStart.assertNotEmpty()
                    intervalUsageEnd.assertNotEmpty()
                    service.assertNotEmpty()
                    compartmentId.assertNotEmpty()
                    compartmentName.assertNotEmpty()
                    region.assertNotEmpty()
                    // product/availabilityDomain can be empty
                    resourceId.assertNotEmpty()
                    billedQuantity.let { assertFalse { it.isNaN() } }
                    // usage/billedQuantityOverage can be empty
                    subscriptionId.assertNotEmpty()
                    productSku.assertNotEmpty()
                    description.assertNotEmpty()
                    // cost/unitPrice can be empty
                    //cost/unitPriceOverage can be empty
                    myCost.let {
                        assertFalse { it.isNaN() }
                        // cost/myCostOverage can be empty
                        // cost/currencyCode can be empty
                        billingUnitReadable.assertNotEmpty()
                        skuUnitDescription.assertNotEmpty()
                        // cost/overageFlag can be empty
                        isCorrection.assertNotEmpty()
                        // lineItem/backreferenceNo can be empty
                        // tags/OKEclusterName can be empty
                        // tags/Oracle-Tags.CreatedBy can be empty
                        // tags/Oracle-Tags.CreatedOn can be empty
                        // tags/orcl-cloud.free-tier-retained can be empty
                    }
                }
            }
        }
    }
}

fun String.assertNotEmpty() {
    assertTrue { this.isNotEmpty() }
}