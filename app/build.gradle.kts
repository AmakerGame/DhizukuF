import org.jetbrains.kotlin.konan.properties.loadProperties
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

// Copies the hub APK (built by :hub, same key and version) into this app's assets.
abstract class CopyHubApk : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val hubApk: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copyApk() {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        val apk = hubApk.files.firstOrNull { it.name.endsWith(".apk") }
            ?: throw GradleException("the hub APK was not built")
        apk.copyTo(File(dir, "dhizuku_compat_hub.apk"), overwrite = true)
    }
}

val keystoreDir: String = "$rootDir/keystore"

val keystoreProps: Properties = Properties()
for (name in arrayOf("r0s.properties", "debug.properties")) {
    val f: File = file("$keystoreDir/$name")
    if (!f.exists()) continue
    keystoreProps.load(f.inputStream())
    break
}

android {
    namespace = "com.EdS.DhizukuF"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 26
        targetSdk = compileSdk
        ndkVersion = "29.0.14206865"

        val versionProps: Properties = org.jetbrains.kotlin.konan.file.File("$rootDir/version.properties").loadProperties()
        versionCode = versionProps.getProperty("versionCode").toInt()
        versionName = versionProps.getProperty("versionName")

        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }

    signingConfigs {
        val storeFile: File = file("$keystoreDir/${keystoreProps.getProperty("storeFile")}")
        val storePassword: String = keystoreProps.getProperty("storePassword")
        val keyAlias: String = keystoreProps.getProperty("keyAlias")
        val keyPassword: String = keystoreProps.getProperty("keyPassword")
        getByName("debug") {
            this.storeFile = storeFile
            this.storePassword = storePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }

    buildTypes {
        configureEach {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    packaging {
        jniLibs.excludes.add("lib/*/libandroidx.graphics.path.so")
        resources.excludes.addAll(arrayOf("META-INF/**", "DebugProbesKt.bin", "kotlin-tooling-metadata.json", "kotlin/**"))
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val hubApkTree = fileTree(project(":hub").layout.buildDirectory.dir("outputs/apk/release")) {
    include("*.apk")
    builtBy(":hub:assembleRelease")
}

androidComponents {
    onVariants { variant ->
        val copyHub = tasks.register<CopyHubApk>("copy${variant.name.replaceFirstChar { it.uppercase() }}HubApk") {
            hubApk.from(hubApkTree)
        }
        variant.sources.assets?.addGeneratedSourceDirectory(copyHub, CopyHubApk::outputDir)
    }
}

dependencies {
    compileOnly(project(":hidden-api"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.ui)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.navigation)
    implementation(libs.compose.materialIcons)
    implementation(libs.material)
    implementation(libs.compose.material3)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.navigationAnimation)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.accompanist.systemuicontroller)

    implementation(libs.lsposed.hiddenapibypass)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.iamr0s.dhizuku.api)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
