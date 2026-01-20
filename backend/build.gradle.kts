group = "pl.everactive"
version = "0.0.1-SNAPSHOT"
description = "backend"

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.kotlinAllOpen)
    alias(libs.plugins.kotlinSerialization)

    alias(libs.plugins.spring)
    alias(libs.plugins.springDependencyManagement)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.shared)

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.flyway.postgresql)

    implementation(libs.spring.boot.starter.oauth2.resource.server)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.konform)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.kotlin.test.junit5)

    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    named<Jar>("jar") {
        enabled = false
    }
}
