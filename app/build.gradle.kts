import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "me.nanova.summaryexpressive"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "me.nanova.summaryexpressive"
        minSdk = 33
        targetSdk = 37
        versionCode = 50
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    flavorDimensions += "distribution"
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

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/INDEX.LIST"
            merges += "META-INF/DEPENDENCIES"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    lint {
        disable.add("MissingTranslation")
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
    }
}

dependencies {
    // https://developer.android.com/develop/ui/compose/bom/bom-mapping
    val composeBomVersion = "2026.06.01"
    val roomVersion = "2.8.4"
    val koogVersion = "1.1.1"
    val koogBetaVersion = "1.1.1-beta"

    // Core & Lifecycle
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.webkit:webkit:1.16.0")

    // DI (Hilt)
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    // Keep alpha override for material expressive features, as intended
    // https://developer.android.com/jetpack/androidx/releases/compose-material3#compose_material3_version_15_2
    implementation("androidx.compose.material3:material3:1.5.0-alpha25")

    // Paging
    implementation("androidx.paging:paging-compose:3.5.0")
    implementation("androidx.paging:paging-runtime-ktx:3.5.0")

    // Data Persistence
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.datastore:datastore:1.2.1")
    implementation("androidx.room:room-runtime:${roomVersion}")
    implementation("androidx.room:room-paging:${roomVersion}")
    implementation("androidx.room:room-ktx:${roomVersion}")
    ksp("androidx.room:room-compiler:${roomVersion}")

    // ML & AI
    // Custom configurations for build flavors to manage ML model packaging
    // Bundles model in APK
    "standaloneImplementation"("com.google.mlkit:text-recognition:16.0.1")
    // Uses Google Play Services
    "gmsImplementation"("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    
    // Koog Stable (Version 1.1.1)
    implementation("ai.koog:koog-agents:${koogVersion}")
    implementation("ai.koog:http-client-ktor:${koogVersion}")
    implementation("ai.koog:prompt-executor-openai-client-android:${koogVersion}")
    implementation("ai.koog:prompt-executor-anthropic-client-android:${koogVersion}")
    implementation("ai.koog:prompt-executor-ollama-client-android:${koogVersion}")
    implementation("ai.koog:prompt-executor-openrouter-client-android:${koogVersion}")
    implementation("ai.koog:prompt-executor-bedrock-client-android:${koogVersion}")

    // Koog only provided the beta version
    implementation("ai.koog:koog-agents-additions:${koogBetaVersion}")
    implementation("ai.koog:prompt-executor-google-client-android:${koogBetaVersion}")
    implementation("ai.koog:prompt-executor-deepseek-client-android:${koogBetaVersion}")
    implementation("ai.koog:prompt-executor-mistralai-client-android:${koogBetaVersion}")
    implementation("ai.koog:prompt-executor-dashscope-client-android:${koogBetaVersion}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Networking
    implementation("io.ktor:ktor-client-android:3.5.2")

    // Serialization & Utilities
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.11.0")
    implementation("org.jsoup:jsoup:1.23.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Debug & Tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    // debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
