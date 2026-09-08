import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    // Sprint 19: Room's annotation processor, via KSP. kapt was tried first and rejected: Room
    // 2.7.1's kapt (javac) backend bundles its own pinned `kotlin-metadata-jvm`, which only
    // understands metadata up to Kotlin's format version 2.2 — this project's Kotlin 2.3.21
    // compiler emits 2.3, so kapt's Room processor fails immediately
    // ("Provided Metadata instance has version 2.3.0, while maximum supported version is
    // 2.2.0"). KSP's processing backend reads Kotlin symbols through the compiler plugin API
    // directly instead of that standalone library, so it doesn't hit the same ceiling.
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val debugApiBaseUrl = providers.environmentVariable("NETPROTECT_API_BASE_URL").orNull
    ?: localProperties.getProperty("NETPROTECT_API_BASE_URL")
    ?: "http://10.0.2.2:8000"

val releaseApiBaseUrl = providers.environmentVariable("NETPROTECT_RELEASE_API_BASE_URL").orNull
    ?: "https://api.netprotect.invalid"

val googleWebClientId = providers.environmentVariable("NETPROTECT_GOOGLE_WEB_CLIENT_ID").orNull
    ?: localProperties.getProperty("NETPROTECT_GOOGLE_WEB_CLIENT_ID")
    ?: ""

android {
    namespace = "com.netprotect.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.netprotect.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-sprint1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", googleWebClientId.asBuildConfigString())
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", debugApiBaseUrl.asBuildConfigString())
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "API_BASE_URL", releaseApiBaseUrl.asBuildConfigString())
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.android)
    // Sprint 18 real-time channel only: plain java.net (used everywhere else in this project,
    // see HttpJsonClient's docstring) has no WebSocket client at all, and
    // java.net.http.WebSocket only reached Android at API 34 — above this project's minSdk 26.
    // Not a reversal of "no networking library" for REST, which java.net already covers.
    implementation(libs.okhttp)

    // Sprint 19: local cache of rules/policy + pending-event queue for offline resilience.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Sprint 19: background heartbeat/usage-sync/pending-event-flush while this app isn't in
    // the foreground — see SupervisedScreen.kt's original comment on why this was deferred here.
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
