import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization")
}

android {
    namespace = "me.nanova.summaryexpressive"
    compileSdk = 36
    flavorDimensions += "distribution"
    androidResources {
        generateLocaleConfig = true
    }
    defaultConfig {
        applicationId = "me.nanova.summaryexpressive"
        minSdk = 33
        targetSdk = 36
        versionCode = 21
        versionName = "0.2.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    productFlavors {
        create("gms") {
            dimension = "distribution"
        }
        create("standalone") {
            dimension = "distribution"
            applicationIdSuffix = ".standalone"
            versionNameSuffix = "-standalone"
        }
    }

    signingConfigs {
        val keyPropertiesFile = rootProject.file("keystore.properties")
        val keyProperties = Properties()
        if (keyPropertiesFile.exists()) {
            keyProperties.load(FileInputStream(keyPropertiesFile))
        }

        create("release") {
            keyAlias = keyProperties.getProperty("keyAlias")
            keyPassword = keyProperties.getProperty("keyPassword")
            storeFile = if (keyProperties.getProperty("storeFile") != null) rootProject.file(keyProperties.getProperty("storeFile")) else null
            storePassword = keyProperties.getProperty("storePassword")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
    lint {
        disable.add("MissingTranslation")
    }
}

dependencies {
    // https://developer.android.com/develop/ui/compose/bom/bom-mapping
    val composeBomVersion = "2025.08.00"

    // Core & Lifecycle
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")

    // DI (Hilt)
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    // Keep alpha override for material expressive features, as intended
    // https://developer.android.com/jetpack/androidx/releases/compose-material3#compose_material3_version_15_2
    implementation("androidx.compose.material3:material3:1.5.0-alpha02")

    // Data Persistence
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // ML & AI
    // Custom configurations for build flavors to manage ML model packaging
    // Bundles model in APK
    "standaloneImplementation"("com.google.mlkit:text-recognition:16.0.1")
    // Uses Google Play Services
    "gmsImplementation"("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("ai.koog:koog-agents:0.3.0") {
        // Exclude CIO engine to use the Android engine provided below
        exclude(group = "io.ktor", module = "ktor-client-cio")
    }

    // Networking
    implementation("io.ktor:ktor-client-android:3.2.3")

    // Serialization & Utilities
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    // Consider consolidating on one JSON parser if possible
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Debug & Tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    // debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
