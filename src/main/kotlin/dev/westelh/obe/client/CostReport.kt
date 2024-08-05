package dev.westelh.obe.client

import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest

private const val namespace: String = "bling"
private const val costReportPrefix: String = "reports/cost-csv"
private const val usageReportPrefix: String = "reports/usage-csv"

fun GetObjectRequest.Builder.billingNamespace(): GetObjectRequest.Builder = this.namespaceName(namespace)

fun GetObjectRequest.Builder.billingBucketName(tenantId: String): GetObjectRequest.Builder = this.bucketName(tenantId)

fun ListObjectsRequest.Builder.billingNamespace(): ListObjectsRequest.Builder = this.namespaceName(namespace)

fun ListObjectsRequest.Builder.billingBucketName(tenantId: String): ListObjectsRequest.Builder = this.bucketName(tenantId)

fun ListObjectsRequest.Builder.billingPrefixForCostReport(): ListObjectsRequest.Builder = this.prefix(costReportPrefix)

fun ListObjectsRequest.Builder.billingPrefixForUsageReport(): ListObjectsRequest.Builder = this.prefix(usageReportPrefix)