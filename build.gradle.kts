plugins {
    kotlin("jvm") version "2.0.21"
    id("io.quarkus") apply false
}

group = "cz.havasi"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

//val quarkusPlatformGroupId: String by project
//val quarkusPlatformArtifactId: String by project
//val quarkusPlatformVersion: String by project
//val coroutinesVersion = "1.10.1"

dependencies {
//    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
//    implementation("io.quarkus:quarkus-scheduler")
//    implementation("io.quarkus:quarkus-config-yaml")
//    implementation("io.quarkus:quarkus-rest-client-jackson")
//    implementation("io.quarkus:quarkus-rest-jackson")
//    implementation("io.quarkus:quarkus-arc") // todo see if needed
//    implementation("io.quarkus:quarkus-kotlin") // todo see if needed
//    implementation("io.quarkus:quarkus-opentelemetry")
//    implementation("io.quarkus:quarkus-oidc")
//    implementation("org.jsoup:jsoup:1.18.3")

//    implementation("io.quarkus:quarkus-mongodb-client")
//    implementation("org.mongodb:bson-kotlin:5.2.1")
//    implementation("io.quarkiverse.mongock:quarkus-mongock:0.6.0")

//    implementation("io.smallrye.reactive:mutiny-kotlin:2.7.0") // todo see if needed
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    // todo add tests
//    testImplementation("io.quarkus:quarkus-junit5")
//    testImplementation("io.rest-assured:rest-assured")
//    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
//    testImplementation("org.testcontainers:testcontainers:1.20.6")
}

//subprojects {
//    kotlin {
//        explicitApi() // Enable explicit API mode
//        jvmToolchain(21)
//    }
//    tasks.test {
//        useJUnitPlatform()
//    }
//}

//kotlin {
//    explicitApi() // Enable explicit API mode
//    jvmToolchain(21)
//}
//tasks.test {
//    useJUnitPlatform()
//}
