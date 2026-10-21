buildscript {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    application
    kotlin("jvm") version "2.2.20"
    id("com.bmuschko.docker-java-application") version "9.4.0"
}

group = "westelh"
version = "main"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin libraries (stdlib is added by the Kotlin JVM plugin).
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.20"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-reflect") {
            version { strictly("2.1.21") }
            because("Preserve the resolved version during BOM migration; align Kotlin versions separately.")
        }
    }

    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:3.74.2"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")
    implementation("com.oracle.oci.sdk:oci-java-sdk-core")
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    constraints {
        implementation("commons-codec:commons-codec:1.19.0") {
            because("commons-codec pulled with oci-java-sdk-common-httpclient-jersey3:3.45.0 has a known vulnerability: Cxeb68d52e-5509")
        }
    }

    implementation(platform("io.prometheus:prometheus-metrics-bom:1.4.1"))
    implementation("io.prometheus:prometheus-metrics-core")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm")
    implementation("io.prometheus:prometheus-metrics-exporter-httpserver")

    implementation("com.github.ajalt.clikt:clikt:5.0.3")

    // logging
    implementation("com.google.flogger:flogger:0.9")
    runtimeOnly("com.google.flogger:flogger-system-backend:0.9")
    implementation(platform("org.slf4j:slf4j-bom:2.0.16"))
    runtimeOnly("org.slf4j:slf4j-jdk14")

    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.9.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")

    // Manage all Jackson versions here, including transitive OCI SDK dependencies.
    // Follow the BOM's compatible version set (annotations uses a minor-only version).
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")

    implementation("com.github.victools:jsonschema-generator:4.37.0")
    implementation("com.github.victools:jsonschema-module-jackson:4.38.0")

    testImplementation(platform("io.kotest:kotest-bom:5.9.1"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("dev.westelh.obe.Main")
}

docker {
    javaApplication {
        baseImage = "gcr.io/distroless/java17-debian12"
        ports = listOf(2112)
    }
}
