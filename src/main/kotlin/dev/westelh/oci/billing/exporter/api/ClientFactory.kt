package dev.westelh.oci.billing.exporter.api

import com.oracle.bmc.Region
import com.oracle.bmc.auth.*
import com.oracle.bmc.objectstorage.ObjectStorageClient

class ClientFactory(private val region: Region, private val auth: AbstractAuthenticationDetailsProvider) : AbstractClientFactory {
    constructor(auth: ConfigFileAuthenticationDetailsProvider): this(auth.region, auth)
    constructor(auth: InstancePrincipalsAuthenticationDetailsProvider): this(auth.region, auth)
    constructor(auth: ResourcePrincipalAuthenticationDetailsProvider): this(auth.region, auth)
    constructor(auth: SessionTokenAuthenticationDetailsProvider): this(auth.region, auth)
    constructor(auth: SimpleAuthenticationDetailsProvider): this(auth.region, auth)

    override fun createClient(): ObjectStorageClient = ObjectStorageClient.builder()
            .region(region)
            .clientConfigurator(GlobalClientConfigurator.configurator)
            .build(auth)
}