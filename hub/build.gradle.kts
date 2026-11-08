import java.util.Properties

// Dhizuku Compatibility Hub: a tiny hidden app (package com.rosan.dhizuku, the id of the original Dhizuku)
// that forwards the Dhizuku API to DhizukuF. It is signed with the SAME key as DhizukuF and has the SAME
// version (both read version.properties), and DhizukuF carries the built APK in its assets.
plugins {
    alias(libs.plugins.agp.app)
}

val keystoreDir: String = "$rootDir/keystore"

val keystoreProps: Properties = Properties()
for (name in arrayOf("r0s.properties", "debug.properties")) {
    val f: File = file("$keystoreDir/$name")
    if (!f.exists()) continue
    keystoreProps.load(f.inputStream())
    break
}

val versionProps: Properties = Properties().apply {
    file("$rootDir/version.properties").inputStream().use { load(it) }
}

android {
    namespace = "com.EdS.DhizukuF.hub"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.rosan.dhizuku"
        minSdk = 26
        targetSdk = compileSdk
        versionCode = versionProps.getProperty("versionCode").toInt()
        versionName = versionProps.getProperty("versionName")
    }

    signingConfigs {
        getByName("debug") {
            this.storeFile = file("$keystoreDir/${keystoreProps.getProperty("storeFile")}")
            this.storePassword = keystoreProps.getProperty("storePassword")
            this.keyAlias = keystoreProps.getProperty("keyAlias")
            this.keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        configureEach {
            // the same key as the app, for every build type
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        // a two-class app; do not let a lint rule block the build of the whole project
        checkReleaseBuilds = false
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
