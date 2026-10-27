package dev.westelh.obe

import com.oracle.bmc.objectstorage.model.ListObjects
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import dev.westelh.obe.config.Config
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class CostReportDownloaderTest : StringSpec({
    "lists all report pages with the expected bucket and prefix" {
        val requests = mutableListOf<ListObjectsRequest>()
        val first = summary("report-20240101.csv.gz")
        val second = summary("report-20240102.csv.gz")
        val downloader = CostReportDownloader(
            config(),
            listObjects = { request ->
                requests += request
                val list = if (requests.size == 1) {
                    ListObjects.builder().objects(listOf(first)).nextStartWith("next-page").build()
                } else {
                    ListObjects.builder().objects(listOf(second)).build()
                }
                ListObjectsResponse.builder().listObjects(list).build()
            },
            getObject = { error("Download should not be requested while listing") }
        )

        val reports = runBlocking { downloader.listAllCostReport() }

        reports!! shouldHaveSize 2
        reports.map { it.name } shouldBe listOf(first.name, second.name)
        requests.map { it.start } shouldBe listOf("", "next-page")
        requests.forEach { request ->
            request.namespaceName shouldBe "bling"
            request.bucketName shouldBe "ocid1.tenancy.oc1..test"
            request.prefix shouldBe "reports/cost-csv"
        }
    }

    "returns null when listing fails and propagates download failures to its nullable result" {
        val downloader = CostReportDownloader(
            config(),
            listObjects = { throw IllegalStateException("offline") },
            getObject = { throw IllegalStateException("offline") }
        )

        runBlocking {
            downloader.listAllCostReport() shouldBe null
            downloader.downloadCostReport("report.csv.gz") shouldBe null
        }
    }

    "builds a get request for the configured tenancy and report name" {
        var received: GetObjectRequest? = null
        val response = GetObjectResponse.builder().build()
        val downloader = CostReportDownloader(
            config(),
            listObjects = { error("List should not be requested") },
            getObject = { request -> received = request; response }
        )

        runBlocking { downloader.downloadCostReport("reports/cost.csv.gz") } shouldBe response
        received!!.namespaceName shouldBe "bling"
        received!!.bucketName shouldBe "ocid1.tenancy.oc1..test"
        received!!.objectName shouldBe "reports/cost.csv.gz"
    }
})

private fun config() = Config.fromYaml("target: ocid1.tenancy.oc1..test".byteInputStream())
private fun summary(name: String) = ObjectSummary.builder().name(name).build()
