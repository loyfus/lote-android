plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "br.com.lote"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.lote"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        // `adb reverse tcp:8080 tcp:8080` maps the device loopback onto the host API.
        // Default points at the Cloudflare Tunnel, so the same URL works on the
        // emulator and on a device. For local development override it:
        //   ./gradlew :app:assembleDebug -PloteApiBaseUrl=http://10.0.2.2:8080
        buildConfigField("String", "API_BASE_URL", "\"${project.findProperty("loteApiBaseUrl") ?: "https://lote.loyfus.com.br"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    implementation(libs.coreKtx)
    implementation(libs.activityCompose)
    implementation(libs.lifecycleRuntimeCompose)
    implementation(libs.lifecycleViewmodelCompose)
    implementation(libs.navigationCompose)

    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeUiGraphics)
    implementation(libs.composeFoundation)
    implementation(libs.composeUiToolingPreview)
    implementation(libs.composeMaterial3)
    implementation(libs.composeMaterialIcons)

    implementation(libs.coilCompose)
    implementation(libs.coilNetworkOkhttp)

    implementation(libs.okhttp)
    implementation(libs.kotlinxSerializationJson)

    debugImplementation(libs.composeUiTooling)

    // Pure-JVM tests for the formatting rules the user actually reads.
    testImplementation(libs.junit)
}
