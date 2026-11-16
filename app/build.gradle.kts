plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val resolvedVersionCode = (findProperty("versionCode") as? String)?.toIntOrNull() ?: 1
val resolvedVersionName = (findProperty("versionName") as? String)?.takeIf { it.isNotBlank() } ?: "0.1.0-dev"

android {
    namespace = "com.gothwad.accounts"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gothwad.accounts"
        minSdk = 26
        targetSdk = 36
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
        manifestPlaceholders["gothwadRedirectScheme"] = "com.gothwad.accounts"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(project(":ui"))
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
}
