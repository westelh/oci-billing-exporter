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
version = "0.0.2"

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
    javaApplication {
        baseImage = "amazoncorretto:21"
    }
}