plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.morninggrace.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.morninggrace.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":alarm"))
    implementation(project(":orchestrator"))
    implementation(project(":bible"))
    implementation(libs.android.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
