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
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:3.32.0"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")
    implementation("com.oracle.oci.sdk:oci-java-sdk-core")
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    implementation("io.prometheus:prometheus-metrics-core:1.1.0")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:1.1.0")
    implementation("io.prometheus:prometheus-metrics-exporter-httpserver:1.1.0")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("com.google.flogger:flogger:0.8")
    implementation("com.google.flogger:flogger-system-backend:0.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.opencsv:opencsv:5.9")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dev.westelh.oci.billing.exporter.app.Main")
}

docker {
    url = "unix:///Users/taisei/.colima/default/docker.sock"
    javaApplication {
        baseImage = "amazoncorretto:21"
    }
}