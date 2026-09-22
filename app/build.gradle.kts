plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zamnimeku.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zamnimeku.app"
        minSdk = 24
        targetSdk = 35
        // Diisi CI saat release (run_number) supaya auto-update bisa
        // bandingkan versi terpasang vs versi terbaru.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Kunci permanen supaya semua build punya tanda tangan sama
    // (update bisa menimpa tanpa "bentrok paket").
    // Password dibaca dari keystore.properties (lokal, jangan commit)
    // atau env KEYSTORE_PASSWORD (diisi GitHub Secrets saat CI).
    val ksProps = java.util.Properties()
    val ksPropsFile = rootProject.file("keystore.properties")
    if (ksPropsFile.exists()) ksPropsFile.inputStream().use { ksProps.load(it) }
    val ksPassword: String? =
        ksProps.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")
    val ksFile = file("release.keystore")

    signingConfigs {
        create("persistent") {
            if (ksFile.exists() && !ksPassword.isNullOrEmpty()) {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = "zamnimeku"
                keyPassword = ksPassword
            } else {
                // Fallback lokal: debug key (jangan dipakai untuk rilis publik)
                storeFile = null
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (ksFile.exists() && !ksPassword.isNullOrEmpty()) {
                signingConfigs.getByName("persistent")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
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
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Image loading
    implementation(libs.coil.compose)

    // Network & Web Scraping
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.android)

    // ExoPlayer / Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Data Storage
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
}
