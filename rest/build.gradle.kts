plugins {
    kotlin("jvm") version "2.0.21"
    id("io.quarkus")
}

group = "cz.havasi.reality.app"
version = "1.0.0-SNAPSHOT"

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-arc")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    implementation(project(":model"))
    implementation(project(":service"))

    testImplementation(kotlin("test"))
}
