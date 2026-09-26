import java.net.URI

plugins { alias(libs.plugins.android.application) }

val productionUrl = providers.gradleProperty("trampoApiUrl").orElse("").get()
require(productionUrl.isEmpty() || (productionUrl.startsWith("https://") && productionUrl.endsWith("/") &&
    !productionUrl.contains("\"") && !productionUrl.contains("\n") && !productionUrl.contains("\r"))) {
    "trampoApiUrl deve ser uma URL HTTPS terminada em /."
}
android {
    namespace = "com.example.trampo"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.example.trampo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    buildTypes {
        debug { buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5165/\"") }
        release {
            optimization { enable = false }
            buildConfigField("String", "API_BASE_URL", "\"$productionUrl\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.arch.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
val validateReleaseApi = tasks.register("validateReleaseApi") {
    val urlToValidate = productionUrl
    doLast {
        val uri = URI(urlToValidate)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.query == null && uri.fragment == null &&
            !uri.host.endsWith(".invalid") && uri.host != "localhost") {
            "Configure a API HTTPS de produção com -PtrampoApiUrl=https://seu-dominio/."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseApi) }