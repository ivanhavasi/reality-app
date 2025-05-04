pluginManagement {
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
    }
}
rootProject.name = "reality-app"

include("application")
include("model")
include("service")
include("mongo")
include("rest")
include("bezrealitky")
include("sreality")
include("idnes")
