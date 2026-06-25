plugins {
    // Kotlin support is built into the Android Gradle Plugin from AGP 9, so the
    // former org.jetbrains.kotlin.android plugin must not be applied. The Compose
    // compiler plugin still applies and is pinned to AGP's bundled Kotlin version.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.saccadacusandroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.saccadacus.android"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Debug builds (the CI default) are unminified; release shrinking is
            // deferred to a later, separately scoped prompt.
            isMinifyEnabled = false
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
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.tensorflow.lite)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
}

// Fetch the MediaPipe Face Landmarker model into assets at build time (prompt 003).
// Downloaded, not committed (see .gitignore); CI fetches it on a fresh checkout.
val faceLandmarkerModelUrl =
    "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task"

val downloadFaceLandmarkerModel = tasks.register("downloadFaceLandmarkerModel") {
    val outFile = file("src/main/assets/face_landmarker.task")
    outputs.file(outFile)
    doLast {
        if (!outFile.exists()) {
            outFile.parentFile.mkdirs()
            val tmp = outFile.parentFile.resolve("face_landmarker.task.tmp")
            uri(faceLandmarkerModelUrl).toURL().openStream().use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            check(tmp.renameTo(outFile)) { "Failed to move downloaded model into place" }
        }
    }
}

tasks.named("preBuild") { dependsOn(downloadFaceLandmarkerModel) }
