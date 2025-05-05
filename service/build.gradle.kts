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
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-arc")

    implementation("io.smallrye.reactive:mutiny-kotlin:2.7.0")

    api(project(":model"))
    testImplementation(kotlin("test"))
}
