pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "DhizukuF"
include("app", "hidden-api")
