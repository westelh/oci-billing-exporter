package dev.westelh.obe.core

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.net.URL

class JacksonCsvParserTest : StringSpec({
    // Preserve all rows and the values and labels needed for metrics when CSV columns change.
    CsvColumnScenarios.cases.forEach { scenario ->
        "${scenario.name} should preserve metric values and labels" {
            val report = scenario.csv.byteInputStream().use { JacksonCsvParser().parse(it) }

            report.items.shouldHaveSize(1)
            val item = report.items.single()
            item.referenceNo shouldBe "csv-regression-000001"
            item.resourceId shouldBe "ocid1.volume.oc1.ap-tokyo-1.synthetic000001"
            item.service shouldBe "BLOCK_STORAGE"
            item.compartmentName shouldBe "csv-regression-test"
            item.region shouldBe "ap-tokyo-1"
            item.availabilityDomain shouldBe "TEST:AP-TOKYO-1-AD-1"
            item.description shouldBe "Synthetic block volume for CSV column regression"
            item.billedQuantity shouldBe 1.25
            item.unitPrice shouldBe 0.5
            item.myCost shouldBe 0.625
            // Missing optional tag columns retain Item's default empty strings.
            val hasTags = scenario.hasTags
            item.OKEclusterName shouldBe if (hasTags) "csv-regression-cluster" else ""
            item.createdBy shouldBe if (hasTags) "synthetic-user" else ""
            item.createdOn shouldBe if (hasTags) "2026-10-01T00:00:00Z" else ""
            item.freeTierRetained shouldBe if (hasTags) "false" else ""
        }
    }

    "extra columns should preserve all 1000 rows and their known values" {
        val resourceURL = this.javaClass.getResource("/reports_cost-extra-columns-large.csv")!!
        val report = resourceURL.openStream().use { JacksonCsvParser().parse(it) }

        report.items.shouldHaveSize(1000)
        report.items.forEachIndexed { index, item ->
            val rowNumber = index + 1
            item.referenceNo shouldBe "csv-regression-${rowNumber.toString().padStart(6, '0')}"
            item.billedQuantity shouldBe rowNumber + 0.25
            item.myCost shouldBe (rowNumber + 0.25) * 0.5
        }
    }

    "parsed item size should match the actual line count" {
        val resourceName = "/reports_cost-csv_00000000012345-00000.csv"
        val resourceURL = this.javaClass.getResource(resourceName)!!

        val parser = JacksonCsvParser()
        resourceURL.openStream().use {
            val item = parser.parse(it)

            // Test case csv has 277 lines
            item.items.shouldHaveSize(277)
        }
    }

    "given csv with extra rows, parser should not throw exceptions" {
        val resourceName = "/reports_cost-csv_00000000012345-00000-extra.csv"
        val resourceURL: URL = this.javaClass.getResource(resourceName)!!

        val parser = JacksonCsvParser()
        resourceURL.openStream().use {
            shouldNotThrowAny {
                parser.parse(it)
            }
        }
    }

    "given more extra rows, parser should not throw exceptions" {
        val resourceName = "/reports_cost-csv_00000000012345-00000-more-extra.csv"
        val resourceURL: URL = this.javaClass.getResource(resourceName)!!

        val parser = JacksonCsvParser()
        resourceURL.openStream().use {
            shouldNotThrowAny {
                parser.parse(it)
            }
        }
    }
})
