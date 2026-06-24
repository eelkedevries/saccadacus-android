// Top-level build file. Plugin versions are declared in gradle/libs.versions.toml
// and applied here with `apply false` so the app module can apply them. Kotlin is
// built into the Android Gradle Plugin from AGP 9, so there is no kotlin.android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
