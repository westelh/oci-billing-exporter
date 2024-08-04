package dev.westelh.obe.config

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider

fun loadFileConfig(config: Config.AuthConfig.FileConfig): ConfigFileAuthenticationDetailsProvider {
    with(config) {
        if (path.isNotBlank()) {
            // Path is different from the default
            return ConfigFileAuthenticationDetailsProvider(path, profile)
        }
        else return ConfigFileAuthenticationDetailsProvider(profile)
    }
}