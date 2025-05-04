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

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}