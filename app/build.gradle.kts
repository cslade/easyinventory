plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kinvo.easyinventory"
    compileSdk = 36 // keep if you want; 34 also fine

    defaultConfig {
        applicationId = "com.kinvo.easyinventory"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🔑 Manifest placeholders used by AndroidManifest.xml
        manifestPlaceholders["appScheme"] = "easyinventory"
        manifestPlaceholders["authPathPrefix"] = "/auth/callback"
        manifestPlaceholders["authHost"] = "app.easyinventory.com" // flavors override below

        // BuildConfig defaults (flavors can override)
        buildConfigField("String",  "TIER", "\"basic\"")
        buildConfigField("String",  "AUTH_BASE_URL", "\"https://app.easyinventory.com\"")
        buildConfigField("String",  "MEMBERSTACK_CALLBACK_PREFIX", "\"https://client.memberstack.com/auth/callback?code=\"")
        buildConfigField("boolean", "IS_PREMIUM", "false")
        buildConfigField("boolean", "IS_DEMO", "false")

        // ✅ New: defaults for Clover + EPOS
        buildConfigField("String",  "CLOVER_BASE_URL", "\"https://api.clover.com\"")
        buildConfigField("String",  "EPOS_AWS_REGION", "\"eu-west-1\"")
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ✅ Flavors (demo/basic/premium)
    flavorDimensions += "tier"
    productFlavors {
        create("demo") {
            dimension = "tier"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"

            buildConfigField("boolean", "IS_BASIC", "false")
            buildConfigField("boolean", "IS_PREMIUM", "false")
            buildConfigField("boolean", "IS_DEMO", "true")
            buildConfigField("String",  "TIER", "\"demo\"")
            buildConfigField("String",  "AUTH_BASE_URL", "\"https://easyinventory.webflow.io\"")

            // Deep link host used by the demo site
            manifestPlaceholders["authHost"] = "easyinventory.webflow.io"

            // Override: point Clover to sandbox in demo
            buildConfigField("String", "CLOVER_BASE_URL", "\"https://apisandbox.dev.clover.com\"")
        }

        create("basic") {
            dimension = "tier"
            applicationIdSuffix = ".basic"
            versionNameSuffix = "-basic"

            buildConfigField("boolean", "IS_BASIC", "true")
            buildConfigField("boolean", "IS_PREMIUM", "false")
            buildConfigField("boolean", "IS_DEMO", "false")
            buildConfigField("String",  "TIER", "\"basic\"")
            buildConfigField("String",  "AUTH_BASE_URL", "\"https://easyinventory.com\"")

            manifestPlaceholders["authHost"] = "easyinventory.com"
            // Clover defaults to prod from defaultConfig
        }

        create("premium") {
            dimension = "tier"
            applicationIdSuffix = ".premium"
            versionNameSuffix = "-premium"

            buildConfigField("boolean", "IS_BASIC", "false")
            buildConfigField("boolean", "IS_PREMIUM", "true")
            buildConfigField("boolean", "IS_DEMO", "false")
            buildConfigField("String",  "TIER", "\"premium\"")
            buildConfigField("String",  "AUTH_BASE_URL", "\"https://easyinventory.com\"")

            manifestPlaceholders["authHost"] = "easyinventory.com"
            // Clover defaults to prod from defaultConfig
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // EncryptedSharedPreferences / MasterKey

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.analytics.impl)
    implementation(libs.volley)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.gson)
    implementation(libs.androidx.print)
    implementation(libs.core)
    implementation("androidx.print:print:1.0.0")
    implementation("com.google.zxing:core:3.5.3")

    // Pick ONE OkHttp line:
    implementation(libs.okhttp)
    // implementation(libs.okhttp.v4100)

    // Helpful for modern WebView APIs
    implementation("androidx.webkit:webkit:1.11.0")
    implementation(libs.firebase.components)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
