plugins {
    kotlin("jvm") version "2.0.21"
    id("io.quarkus") apply false
}

group = "cz.havasi"
version = "1.0.0-SNAPSHOT"

dependencies {
    implementation(project(":application"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    kotlin {
        explicitApi()
        jvmToolchain(21)
    }

    tasks.test {
        useJUnitPlatform()
    }
}
