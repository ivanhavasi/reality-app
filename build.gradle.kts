plugins {
    kotlin("jvm") version "2.0.21"
    id("io.quarkus")
    id("com.vaadin")
}

group = "cz.havasi"
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
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("org.jsoup:jsoup:1.18.3")

    implementation("io.quarkus:quarkus-mongodb-client")
    implementation("org.mongodb:bson-kotlin:5.2.1")

    implementation("io.smallrye.reactive:mutiny-kotlin:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")

    // frontend - vaadin
    implementation(enforcedPlatform("com.vaadin:vaadin-bom:24.6.1"))
    implementation("com.vaadin:vaadin-quarkus-extension:24.6.1") // bom not working?
    implementation("com.vaadin:vaadin-core") {
        exclude(group = "com.vaadin", module = "hilla-dev")
    }
}

kotlin {
    explicitApi() // Enable explicit API mode
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
