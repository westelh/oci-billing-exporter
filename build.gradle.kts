buildscript {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    application
    kotlin("jvm") version "1.9.21"
    id("com.bmuschko.docker-java-application") version "9.4.0"
}

group = "westelh"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:3.45.0"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")
    implementation("com.oracle.oci.sdk:oci-java-sdk-core")
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    constraints {
        implementation("commons-codec:commons-codec:1.13") {
            because("commons-codec pulled with oci-java-sdk-common-httpclient-jersey3:3.45.0 has a known vulnerability: Cxeb68d52e-5509")
        }
    }

    implementation("io.prometheus:prometheus-metrics-core:1.3.1")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:1.1.0")
    implementation("io.prometheus:prometheus-metrics-exporter-httpserver:1.1.0")

    implementation("com.github.ajalt.clikt:clikt:4.2.2")

    // logging
    implementation("com.google.flogger:flogger:0.8")
    runtimeOnly("com.google.flogger:flogger-system-backend:0.8")
    runtimeOnly("org.slf4j:slf4j-jdk14:2.0.13")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")

    implementation("com.github.victools:jsonschema-generator:4.35.0")
    implementation("com.github.victools:jsonschema-module-jackson:4.36.0")

    implementation("com.opencsv:opencsv:5.9")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dev.westelh.obe.Main")
}

docker {
    javaApplication {
        baseImage = "amazoncorretto:21"
    }
}
