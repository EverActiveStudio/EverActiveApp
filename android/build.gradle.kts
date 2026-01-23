plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "pl.everactive"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pl.everactive"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false

            buildConfigField("String", "API_BASE_URL", "\"\"")
        }

        debug {
            buildConfigField("String", "API_BASE_URL", "\"${properties["debugApiBaseUrl"]}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildToolsVersion = "35.0.0"
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project.dependencies.platform(libs.koin.bom))

    implementation(projects.shared)

    implementation(compose.preview)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.uiToolingPreview)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.datastore.preferences)

    // Background service and location
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.koin.android)

    testImplementation(libs.kotlin.test)

    debugImplementation(compose.uiTooling)
}
