package dev.westelh.obe.core

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import java.net.URL

class JacksonCsvParserTest : StringSpec({
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