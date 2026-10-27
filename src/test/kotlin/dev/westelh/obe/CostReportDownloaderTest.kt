package dev.westelh.obe

import com.oracle.bmc.objectstorage.model.ListObjects
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import dev.westelh.obe.client.objectstorage.BillingObjectStorage
import dev.westelh.obe.config.Config
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class CostReportDownloaderTest : StringSpec({
    "lists all report pages with the expected bucket and prefix" {
        val first = summary("report-20240101.csv.gz")
        val second = summary("report-20240102.csv.gz")
        val storage = FakeBillingObjectStorage(
            pages = mapOf("" to listPage(first, nextStartWith = "next-page"), "next-page" to listPage(second))
        )

        val reports = runBlocking { CostReportDownloader(storage, config()).listAllCostReport() }

        reports!! shouldHaveSize 2
        reports.map { it.name } shouldBe listOf(first.name, second.name)
        storage.listRequests.map { it.start } shouldBe listOf("", "next-page")
        storage.listRequests.forEach { request ->
            request.namespaceName shouldBe "bling"
            request.bucketName shouldBe "ocid1.tenancy.oc1..test"
            request.prefix shouldBe "reports/cost-csv"
        }
        storage.getRequests.shouldBeEmpty()
    }

    "returns null when listing or downloading fails" {
        val downloader = CostReportDownloader(FailingBillingObjectStorage(), config())

        runBlocking {
            downloader.listAllCostReport() shouldBe null
            downloader.downloadCostReport("report.csv.gz") shouldBe null
        }
    }

    "builds a get request for the configured tenancy and report name" {
        val response = GetObjectResponse.builder().build()
        val storage = FakeBillingObjectStorage(objects = mapOf("reports/cost.csv.gz" to response))

        runBlocking { CostReportDownloader(storage, config()).downloadCostReport("reports/cost.csv.gz") } shouldBe response
        storage.listRequests.shouldBeEmpty()
        val received = storage.getRequests.single()
        received.namespaceName shouldBe "bling"
        received.bucketName shouldBe "ocid1.tenancy.oc1..test"
        received.objectName shouldBe "reports/cost.csv.gz"
    }
})

// Serves list pages keyed by their start token and objects keyed by name, recording every request
private class FakeBillingObjectStorage(
    private val pages: Map<String, ListObjects> = emptyMap(),
    private val objects: Map<String, GetObjectResponse> = emptyMap()
) : BillingObjectStorage {
    val listRequests = mutableListOf<ListObjectsRequest>()
    val getRequests = mutableListOf<GetObjectRequest>()

    override suspend fun listObjects(request: ListObjectsRequest): ListObjectsResponse {
        listRequests += request
        val page = pages[request.start ?: ""] ?: error("No page for start=${request.start}")
        return ListObjectsResponse.builder().listObjects(page).build()
    }

    override suspend fun getObject(request: GetObjectRequest): GetObjectResponse {
        getRequests += request
        return objects[request.objectName] ?: error("No object named ${request.objectName}")
    }
}

private class FailingBillingObjectStorage : BillingObjectStorage {
    override suspend fun listObjects(request: ListObjectsRequest): ListObjectsResponse = throw IllegalStateException("offline")
    override suspend fun getObject(request: GetObjectRequest): GetObjectResponse = throw IllegalStateException("offline")
}

private fun config() = Config.fromYaml("target: ocid1.tenancy.oc1..test".byteInputStream())
private fun summary(name: String) = ObjectSummary.builder().name(name).build()
private fun listPage(vararg objects: ObjectSummary, nextStartWith: String? = null) =
    ListObjects.builder().objects(objects.toList()).nextStartWith(nextStartWith).build()
