plugins {
    kotlin("jvm") version "2.0.21"
}

group = "cz.havasi.reality.app"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    testImplementation(kotlin("test"))
}

kotlin {
    explicitApi() // Enable explicit API mode
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}