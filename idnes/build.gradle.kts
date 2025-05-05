plugins {
    kotlin("jvm") version "2.0.21"
}

group = "cz.havasi.reality.app"
version = "1.0.0-SNAPSHOT"

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-arc")

    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    implementation(project(":model"))
    implementation(project(":service"))

    testImplementation(kotlin("test"))
}
