plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}
group = "com.github.Bansify.accountsapp"
version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")
android {
    namespace = "com.bansify.accounts.core"
    compileSdk = 36
    defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    api(libs.coroutines)
    api(libs.okhttp)
    implementation(libs.browser)
    implementation(libs.serialization)
    implementation(libs.tink)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.okhttp.tls)
}
afterEvaluate {
    publishing { publications { register<MavenPublication>("release") {
        from(components["release"])
        groupId = "com.github.Bansify.accountsapp"
        artifactId = "core"
        version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")
    } } }
}
