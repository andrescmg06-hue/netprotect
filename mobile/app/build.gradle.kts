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

// Environment first (CI), then local.properties (a developer machine); blank counts as unset.
fun buildSetting(name: String): String? =
    (providers.environmentVariable(name).orNull ?: localProperties.getProperty(name))
        ?.takeIf { it.isNotBlank() }

val debugApiBaseUrl = buildSetting("NETPROTECT_API_BASE_URL") ?: "http://10.0.2.2:8000"

// Sprint 71: the `.invalid` fallback only lets Gradle configure the release variant; the
// taskGraph check at the bottom refuses to package a release that still uses it.
val releaseApiBaseUrlSetting = buildSetting("NETPROTECT_RELEASE_API_BASE_URL")
val releaseApiBaseUrl = releaseApiBaseUrlSetting ?: "https://api.netprotect.invalid"

val googleWebClientId = buildSetting("NETPROTECT_GOOGLE_WEB_CLIENT_ID") ?: ""

// Sprint 71: release signing comes only from these four settings, never from a keystore in the
// repository. All four or none: a partial set is reported by the release check below.
val releaseSigningSettings = listOf(
    "NETPROTECT_RELEASE_KEYSTORE_FILE",
    "NETPROTECT_RELEASE_KEYSTORE_PASSWORD",
    "NETPROTECT_RELEASE_KEY_ALIAS",
    "NETPROTECT_RELEASE_KEY_PASSWORD",
).associateWith { buildSetting(it) }
val releaseKeystoreFile = releaseSigningSettings["NETPROTECT_RELEASE_KEYSTORE_FILE"]?.let { file(it) }
val hasReleaseSigning = releaseSigningSettings.values.all { it != null } && releaseKeystoreFile!!.isFile

// Sprint 71: an installed APK only accepts an update with a higher versionCode, so each release
// must set its own. Unset keeps the previous fixed values, so debug builds don't change.
val appVersionCode = providers.environmentVariable("NETPROTECT_VERSION_CODE").orNull?.let { raw ->
    raw.trim().toIntOrNull()?.takeIf { it > 0 }
        ?: throw GradleException("NETPROTECT_VERSION_CODE must be a positive integer, got '$raw'.")
} ?: 1
val appVersionName = providers.environmentVariable("NETPROTECT_VERSION_NAME").orNull
    ?.takeIf { it.isNotBlank() } ?: "0.1.0-sprint1"

android {
    namespace = "com.netprotect.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.netprotect.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", googleWebClientId.asBuildConfigString())
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseSigningSettings["NETPROTECT_RELEASE_KEYSTORE_PASSWORD"]
                keyAlias = releaseSigningSettings["NETPROTECT_RELEASE_KEY_ALIAS"]
                keyPassword = releaseSigningSettings["NETPROTECT_RELEASE_KEY_PASSWORD"]
            }
        }
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // Sprint 51: the release build (R8 on) pointed at the local backend and signed with the debug
        // key, so the minified app can be installed and exercised end to end. Never distributed.
        create("minified") {
            initWith(getByName("release"))
            buildConfigField("String", "API_BASE_URL", debugApiBaseUrl.asBuildConfigString())
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    // Same cleartext exception as debug (10.0.2.2 and localhost only), without the debug gallery.
    sourceSets.getByName("minified").res.srcDir("src/debug/res")

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

    // Sprint 23: WebRTC (screen sharing). Google stopped publishing its own prebuilt
    // `org.webrtc:google-webrtc` AAR years ago (last release 1.0.32006, JCenter-era), so there is
    // no first-party artifact to depend on; GetStream's fork tracks upstream libwebrtc and keeps
    // the same `org.webrtc.*` API, including ScreenCapturerAndroid, which is what turns a
    // MediaProjection into a WebRTC video track. Building libwebrtc from source was the only
    // other option and is not a reasonable dependency for this project. Unlike okhttp/Room, this
    // one ships native .so libraries — it is the largest dependency in the app by far, accepted
    // because peer-to-peer video has no java.net-shaped alternative at all.
    implementation(libs.stream.webrtc.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    // Sprint 25: real Room instrumented tests for the Sprint 19 offline cache — an in-memory
    // Room database still needs a real SQLite from the Android runtime, which is exactly what
    // room-testing's Room.inMemoryDatabaseBuilder() gives an androidTest, unlike a JVM unit test.
    androidTestImplementation(libs.androidx.room.testing)
    // Sprint 42 (D-13): Compose UI tests (screen states, component gallery). Test-only artifacts,
    // versions come from the Compose BOM above; ui-test-manifest adds the empty activity the
    // tests host their content in, and only to debug builds.
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// 'minified' is debug-signed with a cleartext exception: no APK or bundle of it may come out of
// CI, where it could be mistaken for a release (S51 security review). Its unit tests may still run.
gradle.taskGraph.whenReady {
    // Only the tasks that produce the final APK/AAB: unit tests depend on internal
    // package*/bundle* tasks of the variant (resources, class jars) and must keep running.
    val artifactTasks = setOf("assembleMinified", "packageMinified", "bundleMinified", "packageMinifiedBundle")
    val packagesMinified = allTasks.any { it.project == project && it.name in artifactTasks }
    if (System.getenv("CI") != null && packagesMinified) {
        throw GradleException("The 'minified' build type is for local testing only and cannot be packaged in CI.")
    }
}

// Sprint 71: a release APK/AAB that points at the `.invalid` placeholder, has no Google client ID
// or is unsigned would install and then fail for real users, so packaging one fails the build
// instead. Checked only when a release artifact task is actually scheduled — same reasoning as
// the 'minified' guard above — so debug, test, lint and 'minified' builds never need these values.
gradle.taskGraph.whenReady {
    val releaseArtifactTasks = setOf("assembleRelease", "packageRelease", "bundleRelease", "packageReleaseBundle")
    if (allTasks.none { it.project == project && it.name in releaseArtifactTasks }) return@whenReady

    val problems = mutableListOf<String>()
    when {
        releaseApiBaseUrlSetting == null ->
            problems += "NETPROTECT_RELEASE_API_BASE_URL is not set (the production API URL)."
        !releaseApiBaseUrlSetting.startsWith("https://") ->
            problems += "NETPROTECT_RELEASE_API_BASE_URL must start with https:// (release disables cleartext)."
        Regex("""\.(invalid|example)\b|\bexample\.(com|net|org)\b""").containsMatchIn(releaseApiBaseUrlSetting) ->
            problems += "NETPROTECT_RELEASE_API_BASE_URL is still an example domain: $releaseApiBaseUrlSetting"
    }
    if (googleWebClientId.isBlank()) {
        problems += "NETPROTECT_GOOGLE_WEB_CLIENT_ID is not set (the production Google web client ID)."
    } else if (googleWebClientId.startsWith("tu-id-de-cliente")) {
        problems += "NETPROTECT_GOOGLE_WEB_CLIENT_ID is still the value from local.properties.example."
    }
    releaseSigningSettings.filterValues { it == null }.keys.forEach {
        problems += "$it is not set (release signing)."
    }
    if (releaseKeystoreFile != null && !releaseKeystoreFile.isFile) {
        problems += "NETPROTECT_RELEASE_KEYSTORE_FILE points to a missing file: ${releaseKeystoreFile.path}"
    }
    if (problems.isNotEmpty()) {
        throw GradleException("Cannot package a release build:\n- " + problems.joinToString("\n- "))
    }
}
