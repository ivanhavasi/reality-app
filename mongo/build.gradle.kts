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
    implementation("io.quarkus:quarkus-mongodb-client")
    implementation("org.mongodb:bson-kotlin:5.2.1")
    implementation("io.quarkiverse.mongock:quarkus-mongock:0.6.0")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-arc")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    implementation(project(":model"))
    implementation(project(":service"))
}
