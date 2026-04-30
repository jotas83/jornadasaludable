// Top-level build file: aplica los plugins a nivel proyecto sin activarlos.
// Cada submódulo decide cuáles de estos plugins aplica.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.kapt)         apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.navigation.safeargs) apply false
}
