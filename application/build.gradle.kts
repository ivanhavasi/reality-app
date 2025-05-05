plugins {
    kotlin("jvm") version "2.0.21"
    id("io.quarkus")
}

group = "cz.havasi.reality.app"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-arc")

    implementation(project(":model"))
    implementation(project(":service"))
    implementation(project(":rest"))
    implementation(project(":mongo"))
    implementation(project(":bezrealitky"))
    implementation(project(":sreality"))
    implementation(project(":idnes"))

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("org.testcontainers:testcontainers:1.20.6")
    testImplementation("io.quarkus:quarkus-mongodb-client")
    testImplementation("org.mongodb:bson-kotlin:5.2.1")
    testImplementation("io.smallrye.reactive:mutiny-kotlin:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}