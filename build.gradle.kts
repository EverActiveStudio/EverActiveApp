plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJpa) apply false
    alias(libs.plugins.kotlinSpring) apply false
    alias(libs.plugins.kotlinAllOpen) apply false

    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKMPLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false

    alias(libs.plugins.spring) apply false
    alias(libs.plugins.springDependencyManagement) apply false
}
