package dev.westelh.oci.billing.exporter.app

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider

typealias AbstractADP = AbstractAuthenticationDetailsProvider

class ClientSecret(impl: AbstractADP) : AbstractADP by impl