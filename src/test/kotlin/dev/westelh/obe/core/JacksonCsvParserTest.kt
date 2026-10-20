package dev.westelh.obe.core

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.net.URL

class JacksonCsvParserTest : StringSpec({
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
